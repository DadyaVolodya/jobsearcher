const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const { setup } = require('./proctor-navigation.cjs');

(async () => {
  const browser = await chromium.launch({
    ...(process.env.PROCTOR_BROWSER_PATH ? { executablePath: process.env.PROCTOR_BROWSER_PATH } : { channel: 'chrome' }),
    headless: true,
  });
  try {
    let messageRequests = 0;
    const messages = [{ id: 'message-1', senderUserId: 'employer', body: 'Initial message', createdAt: new Date().toISOString() }];
    const page = await setup(browser, 5173, 1440, endpoint => {
      if (endpoint === '/chats') return [{ id: 'thread-1', companyName: 'Company', salaryFrom: 100000, salaryTo: 150000, invitationStatus: 'SENT', unreadCount: 0 }];
      if (endpoint === '/chats/unread') return { unreadTotal: 0, threads: [] };
      if (endpoint === '/chats/thread-1/messages') { messageRequests++; return messages; }
    });
    await page.goto('http://localhost:5173/candidate/#chats', { waitUntil: 'networkidle' });
    await page.getByText('Initial message', { exact: false }).waitFor();
    const input = page.locator('.chat-compose input');
    await input.fill('Unsent draft');
    await input.focus();
    messages.push({ id: 'message-2', senderUserId: 'employer', body: 'Incoming message', createdAt: new Date().toISOString() });
    await page.getByText('Incoming message', { exact: false }).waitFor({ timeout: 10000 });
    assert.equal(await input.inputValue(), 'Unsent draft');
    assert.equal(await input.evaluate(e => e === document.activeElement), true, 'Polling must preserve focus');
    await page.evaluate(() => { location.hash = 'overview'; });
    await page.locator('.chat-compose').waitFor({ state: 'detached' });
    const count = messageRequests;
    await page.waitForTimeout(5500);
    assert.equal(messageRequests, count, 'Polling must stop when the chat closes');
    console.log('Chat checks passed: incoming messages, draft/focus preserved, polling cleanup.');
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exit(1); });
