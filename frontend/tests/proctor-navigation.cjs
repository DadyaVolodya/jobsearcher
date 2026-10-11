const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const assert = require('node:assert/strict');

// Serve the built cabinet and a deterministic API through browser interception.
// No HTTP listeners or running backend are needed.

const session = { id: 'test-1', status: 'IN_PROGRESS', items: [{ itemId: 'item-1', code: 'q1', section: 'A', gradeLevel: 'JUNIOR', prompt: 'Test question', options: [{ code: 'yes', label: 'Yes' }], params: {} }], answers: {}, score: null, failReason: null, sectionScores: {} };
const completed = { ...session, status: 'COMPLETED', score: 1, assignedGrade: 'JUNIOR', gradeConfirmed: true };
const proctors = [];
let submitFails = false;
async function setup(browser, port, width = 1440) {
  const role = port === 5173 ? 'CANDIDATE' : 'EMPLOYER';
  const page = await browser.newPage({ viewport: { width, height: 1000 } });
  await page.route('http://localhost:' + port + '/**', async route => {
    const url = new URL(route.request().url());
    if (url.pathname.startsWith('/api/v1')) {
      const endpoint = url.pathname.slice(7);
      let data = [];
      if (endpoint === '/me') data = { userId: 'user-1', email: role === 'CANDIDATE' ? 'candidate1@example.com' : 'employer1@example.com', role, emailVerified: true, consents: [] };
      if (endpoint === '/candidate/profile') data = { fullName: null, claimedGrade: 'JUNIOR', stack: [], softSkills: [] };
      if (endpoint === '/employer/profile') data = { companyName: 'New company' };
      if (endpoint === '/surveys') data = [{ id: 'survey-1', code: 'IT_CANDIDATE', title: 'Survey' }];
      if (endpoint === '/surveys/survey-1/questions') data = [{ id: 'q-1', code: 'q', text: 'Question', sortOrder: 1, options: [{ code: 'yes', label: 'Yes' }] }];
      if (endpoint === '/surveys/sessions') data = { id: 'survey-session-1', status: 'IN_PROGRESS' };
      if (endpoint.endsWith('/complete')) data = { id: 'survey-session-1', status: 'COMPLETED' };
      if (endpoint === '/tests/sessions') { await new Promise(r => setTimeout(r, 300)); data = session; }
      if (endpoint === '/tests/sessions/test-1') data = session;
      if (endpoint.endsWith('/proctor')) { proctors.push(route.request().postDataJSON().event); data = { ...completed, failReason: 'LEFT_WINDOW', score: 0, gradeConfirmed: false }; }
      if (endpoint.endsWith('/submit')) {
        if (submitFails) return route.fulfill({ status: 500, contentType: 'application/json', body: '{}' });
        data = completed;
      }
      return route.fulfill({ contentType: 'application/json', body: JSON.stringify(data) });
    }
    const app = role === 'CANDIDATE' ? 'candidate' : 'employer';
    const file = path.join(__dirname, '..', 'apps', app, 'dist', url.pathname === '/' ? 'index.html' : url.pathname);
    const mime = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.png': 'image/png', '.woff2': 'font/woff2' }[path.extname(file)];
    if (!fs.existsSync(file)) return route.fulfill({ status: 404 });
    await route.fulfill({ contentType: mime, body: fs.readFileSync(file) });
  });
  return page;
}
async function startTest(page) {
  await page.goto('http://localhost:5173/#testing', { waitUntil: 'networkidle' });
  await page.getByRole('button', { name: '\u041d\u0430\u0447\u0430\u0442\u044c \u0441 \u0430\u043d\u043a\u0435\u0442\u044b' }).click();
  await page.locator('input[type=radio]').check();
  await page.locator('.form-actions .button.primary').click();
  await page.getByRole('button', { name: /^\u041d\u0430\u0447\u0430\u0442\u044c \u0442\u0435\u0441\u0442/ }).click();
  assert.equal(await page.locator('.nav-link:not(:disabled)').count(), 0, 'Navigation must lock while starting');
  await page.locator('.test-screen').waitFor();
}
(async () => {
  const browser = await chromium.launch({
    ...(process.env.PROCTOR_BROWSER_PATH ? { executablePath: process.env.PROCTOR_BROWSER_PATH } : { channel: 'chrome' }),
    headless: true,
  });
  try {
    const page = await setup(browser, 5173);
    const errors = [];
    page.on('pageerror', e => errors.push(e.message));
    await startTest(page);
    assert.equal(await page.locator('.user-popover button:not(:disabled)').count(), 0);
    await page.evaluate(() => { location.hash = 'profile'; });
    await page.waitForTimeout(100);
    assert.equal(new URL(page.url()).hash, '#testing');
    assert.equal(await page.locator('.test-screen').count(), 1);
    await page.evaluate(() => history.back());
    await page.waitForTimeout(100);
    assert.equal(new URL(page.url()).hash, '#testing');
    assert.equal(await page.locator('.test-screen').count(), 1);
    submitFails = true;
    await page.locator('.test-screen .form-actions .button.primary').click();
    await page.waitForTimeout(100);
    assert.equal(await page.locator('.nav-link:not(:disabled)').count(), 0, 'Failed submission must retain lock');
    submitFails = false;
    await page.locator('.test-screen .form-actions .button.primary').click();
    await page.waitForTimeout(100);
    assert.equal(await page.locator('.nav-link:disabled').count(), 0, 'Successful submission must unlock');
    assert.equal(proctors.length, 0, 'Blocked in-app navigation must preserve the attempt');
    assert.deepEqual(errors, []);
    await page.close();
    const leaving = await setup(browser, 5173);
    await startTest(leaving);
    await leaving.evaluate(() => window.dispatchEvent(new PageTransitionEvent('pagehide')));
    await leaving.waitForTimeout(200);
    assert.deepEqual(proctors, ['LEAVE_WINDOW']);
    assert.equal(await leaving.locator('.nav-link:disabled').count(), 0);
    await leaving.close();
    console.log('Proctor checks passed: pending start, menu/account lock, hash/history guard, failed/successful submit, pagehide.');

  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exit(1); });
