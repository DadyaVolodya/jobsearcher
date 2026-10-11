export type Grade = 'JUNIOR' | 'MIDDLE' | 'SENIOR' | 'INTERN';
export type Role = 'CANDIDATE' | 'EMPLOYER';
export type Id = string;
export type ConsentType = 'PERSONAL_DATA_PROCESSING' | 'PROFILE_PUBLICATION';
export interface Consent { id: Id; consentType: ConsentType; granted: boolean; documentTitle?: string; documentUrl?: string; currentDocumentVersion?: string; versionLabel?: string }
export interface Me { userId: Id; email: string; role: Role; emailVerified: boolean; consents: Consent[] }
export interface CandidateProfile {
  id: Id; fullName: string | null; phone: string | null; city: string | null; about: string | null;
  stack: string[]; softSkills: string[]; experienceYears: number | null; resumeText: string | null;
  privacyHideContacts: boolean; claimedGrade: Grade | null; assignedGrade: Grade | null;
  gradeConfirmed: boolean; industryCode: string | null; specializationCode: string | null;
  categoryCode: string | null; fspParticipantId: string | null; fspGrade: Grade | null;
  testScore: number; profileScore: number; published: boolean;
}
export interface FspAchievement { id: Id; title: string; eventName: string | null; place: number | null; points: number; verified: boolean }
export interface FspSummary { fspParticipantId: string | null; fspGrade: Grade | null; totalPoints: number; achievementsCount: number; achievements: FspAchievement[] }
export interface EmployerProfile { id: Id; companyName: string | null; description: string | null; industryFocus: string | null; contactEmail: string | null; contactPhone: string | null; website: string | null }
export interface Need { id: Id; title: string; description: string; industryCode: string; specializationCode: string; grade: Grade; stack: string[]; salaryFrom: number; salaryTo: number; active: boolean }
export interface Vacancy { id: Id; title: string; description: string; industryCode: string; specializationCode: string; grade: Grade; stack: string[]; salaryFrom: number; salaryTo: number; status: string; createdAt: string }
export interface Application {
  id: Id; vacancyId: Id; candidateId: Id; coverLetter: string; status: string; createdAt: string;
  candidateName?: string | null; candidateEmail?: string | null; candidatePhone?: string | null;
  contactsRevealed?: boolean; assignedGrade?: Grade | null; gradeConfirmed?: boolean | null;
}
export interface CandidateView { id: Id; fullName: string | null; categoryCode: string | null; industryCode: string | null; specializationCode: string | null; grade: Grade | null; stack: string[]; testScore: number; profileScore: number; matchScore: number; fspPoints: number; fspGrade: Grade | null; fspAchievementsCount: number; fspLinked: boolean; city: string | null }
export interface RankedCandidate { candidateId: Id; categoryCode: string; score: number; fspPoints: number; fspGrade: Grade | null; fspLinked: boolean; explain: string[] }
export interface MatchResponse { needId: Id; primaryCategory: string; candidates: RankedCandidate[]; categories: Record<string, RankedCandidate[]> }
export interface Invitation { id: Id; chatThreadId: Id | null; companyName: string; candidateId: Id; candidateName: string; candidateEmail: string | null; candidatePhone: string | null; needId: Id | null; vacancyId: Id | null; message: string; salaryFrom: number; salaryTo: number; status: string; declineReason: string | null; createdAt: string }
export interface Thread { id: Id; invitationId: Id; vacancyId: Id | null; companyName: string; candidateName: string; invitationStatus: string; salaryFrom: number; salaryTo: number; declineReason: string | null; contactsRevealed: boolean; candidateEmail: string | null; candidatePhone: string | null; employerEmail: string | null; employerPhone: string | null; unreadCount: number; createdAt: string }
export interface UnreadSummary { unreadTotal: number; threads: { threadId: Id; unreadCount: number }[] }
export interface ChatMessage { id: Id; body: string; senderUserId: Id; createdAt: string }
export interface Industry { code: string; name: string }
export interface Specialization { code: string; name: string; industryCode: string }
export interface Questionnaire { id: Id; code: string; title: string }
export interface SurveyQuestion { id: Id; code: string; text: string; questionType: string; options: { code: string; label: string }[]; sortOrder: number }
export interface SurveySession { id: Id; status: string; answers: Record<string, unknown> }
export interface TestItem { itemId: Id; code: string; section: 'A' | 'B' | 'C'; gradeLevel: Grade; prompt: string; antiAiPrompt: string | null; captchaStyle: boolean; copyPasteBlocked: boolean; options: { code: string; label: string }[]; params: Record<string, unknown> }
export interface TestSession { id: Id; status: string; items: TestItem[]; answers: Record<string, unknown>; score: number | null; passed: boolean | null; failReason: string | null; gradeConfirmed: boolean | null; assignedGrade: Grade | null; suggestedRetryGrade?: Grade | null; sectionScores: Record<string, number>; startedAt: string }
