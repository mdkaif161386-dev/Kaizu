const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTimestamp() {
  return new Date(Date.now() - 1000);
}

function validUserProfile(uid) {
  const ts = nowTimestamp();
  return {
    userId: uid,
    fullName: "Aarav Sharma",
    dob: "2005-08-14",
    phoneNumber: "+91 9876543210",
    selectedExamId: "NEET",
    totalPoints: 0,
    quizzesCompleted: 0,
    totalQuestionsSolved: 0,
    totalCorrectAnswers: 0,
    megaQuizzesCompleted: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validQuizAttempt(uid) {
  return {
    userId: uid,
    examId: "NEET",
    examName: "NEET UG",
    mode: "CHAPTER",
    subjectNames: ["Physics"],
    chapterSummary: "Electrostatics",
    difficulty: "HARD",
    totalQuestions: 5,
    correctCount: 4,
    wrongCount: 1,
    skippedCount: 0,
    scorePoints: 160,
    accuracyPercent: 80.0,
    timePerQuestionSec: 45,
    timeTakenSec: 120,
    questionReviews: [
      {
        question: "Sample Q",
        options: ["A", "B", "C", "D"],
        correctIndex: 0,
        selectedIndex: 0,
        explanation: "Detailed explanation",
        subject: "Physics",
        chapter: "Electrostatics",
        isCorrect: true,
      },
    ],
    createdAt: nowTimestamp(),
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID))
  );
});

test("Authenticated user can create, read, and update their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profile = validUserProfile(ALICE_UID);
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(profile));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      selectedExamId: "JEE",
      totalPoints: 200,
      updatedAt: nowTimestamp(),
    })
  );
});

test("Authenticated user cannot read or overwrite another user's profile (PII protection)", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set(validUserProfile(BOB_UID));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set(validUserProfile(BOB_UID))
  );
  await assertFails(aliceDb.collection("users").get());
});

test("Shadow update with ghost field on user profile is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserProfile(ALICE_UID))
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: nowTimestamp(),
    })
  );
});

test("Quiz attempts can be created and listed by owner, but are immutable and isolated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const attemptRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("quiz_attempts")
    .doc("attempt_1");

  await assertSucceeds(attemptRef.set(validQuizAttempt(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quiz_attempts").get()
  );
  // Immutable: update rejected
  await assertFails(attemptRef.update({ scorePoints: 9999 }));
  // Cross-user read rejected
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("quiz_attempts").get()
  );
});

test("Exam and Mega leaderboards allow signed-in reads but restrict writes to owner uid", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  const lbRef = aliceDb.collection("leaderboards").doc("NEET").collection("entries").doc(ALICE_UID);
  await assertSucceeds(
    lbRef.set({
      userId: ALICE_UID,
      displayName: "Aarav S",
      examId: "NEET",
      examName: "NEET UG",
      scorePoints: 320,
      bestAccuracy: 90.0,
      quizzesCount: 2,
      updatedAt: nowTimestamp(),
    })
  );

  // Signed-in Bob can read NEET leaderboard
  await assertSucceeds(
    bobDb.collection("leaderboards").doc("NEET").collection("entries").get()
  );
  // Bob cannot tamper with Alice's leaderboard entry
  await assertFails(
    bobDb.collection("leaderboards").doc("NEET").collection("entries").doc(ALICE_UID).update({
      scorePoints: 0,
      updatedAt: nowTimestamp(),
    })
  );
  // Unauthenticated cannot read leaderboard
  await assertFails(
    unauthDb.collection("leaderboards").doc("NEET").collection("entries").get()
  );

  // Mega Leaderboard test
  const megaRef = aliceDb.collection("mega_leaderboards").doc("NEET").collection("entries").doc(ALICE_UID);
  await assertSucceeds(
    megaRef.set({
      userId: ALICE_UID,
      displayName: "Aarav S",
      examId: "NEET",
      examName: "NEET UG",
      scorePoints: 650,
      bestAccuracy: 95.0,
      megaQuizzesCount: 1,
      highestDifficulty: "EXTREME",
      bestTimeSec: 185,
      updatedAt: nowTimestamp(),
    })
  );
  await assertSucceeds(
    bobDb.collection("mega_leaderboards").doc("NEET").collection("entries").get()
  );
  await assertFails(
    bobDb.collection("mega_leaderboards").doc("NEET").collection("entries").doc(ALICE_UID).update({
      scorePoints: 10,
      updatedAt: nowTimestamp(),
    })
  );
});

test("Daily study goal is strictly isolated to owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const goalRef = aliceDb.collection("users").doc(ALICE_UID).collection("study_goals").doc("2026-11-15");

  await assertSucceeds(
    goalRef.set({
      userId: ALICE_UID,
      dateKey: "2026-11-15",
      targetMinutes: 180,
      loggedSeconds: 1200,
      activeSessionStartedAtMs: 0,
      cycleAnchorEpochMs: 1700000000000,
      activeSubject: "Physics",
      blockedPackages: ["com.instagram.android"],
      updatedAt: nowTimestamp(),
    })
  );
  await assertSucceeds(goalRef.get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("study_goals").doc("2026-11-15").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("study_goals").doc("2026-11-15").set({
      userId: ALICE_UID,
      dateKey: "2026-11-15",
      targetMinutes: 15,
      loggedSeconds: 0,
      activeSessionStartedAtMs: 0,
      cycleAnchorEpochMs: 0,
      activeSubject: "",
      blockedPackages: [],
      updatedAt: nowTimestamp(),
    })
  );
});

test("Study time leaderboard allows signed-in reads and restricts writes to owner", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertSucceeds(
    aliceDb.collection("study_time_leaderboard").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Aarav S",
      examId: "NEET",
      examName: "NEET UG",
      dayKey: "2026-11-15",
      daySeconds: 3600,
      weekKey: "2026-W46",
      weekSeconds: 14400,
      monthKey: "2026-11",
      monthSeconds: 43200,
      totalSeconds: 43200,
      updatedAt: nowTimestamp(),
    })
  );

  await assertSucceeds(bobDb.collection("study_time_leaderboard").get());
  await assertFails(unauthDb.collection("study_time_leaderboard").get());
  await assertFails(
    bobDb.collection("study_time_leaderboard").doc(ALICE_UID).update({
      daySeconds: 0,
      updatedAt: nowTimestamp(),
    })
  );
});

test("Chat moderation enforces 7-day chat ban across exam and direct chats and prevents self-unban or delete", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const futureBan = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000);
  const ts = nowTimestamp();

  // Unbanned Alice can message in Exam Study Room
  await assertSucceeds(
    aliceDb.collection("exam_chats").doc("NEET").collection("messages").doc("msg_1").set({
      senderId: ALICE_UID,
      senderName: "Aarav S",
      examId: "NEET",
      threadId: "",
      participantIds: [],
      text: "How to solve Gauss Law problems quickly?",
      attachmentLabel: "",
      attachmentType: "NONE",
      createdAt: ts,
    })
  );

  // Unbanned Alice can start/send 1-on-1 DM with Bob, Bob can read it, Alice cannot spoof Bob as sender
  const threadId = `${ALICE_UID}__${BOB_UID}`;
  await assertSucceeds(
    aliceDb.collection("direct_chat_threads").doc(threadId).set({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Aarav", [BOB_UID]: "Bob" },
      lastMessage: "Hey let us revise Biology",
      lastSenderId: ALICE_UID,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("direct_chats").doc(threadId).collection("messages").doc("dm_1").set({
      senderId: ALICE_UID,
      senderName: "Aarav",
      examId: "NEET",
      threadId,
      participantIds: [ALICE_UID, BOB_UID],
      text: "Hey let us revise Biology",
      attachmentLabel: "",
      attachmentType: "NONE",
      createdAt: ts,
    })
  );
  await assertSucceeds(
    bobDb
      .collection("direct_chats")
      .doc(threadId)
      .collection("messages")
      .where("participantIds", "==", [ALICE_UID, BOB_UID])
      .get()
  );

  // Record 4th strike -> 1 week ban
  await assertSucceeds(
    aliceDb.collection("chat_moderation").doc(ALICE_UID).set({
      userId: ALICE_UID,
      warningCount: 4,
      lastWarningReason: "Adult video prohibited",
      bannedUntilEpochMs: futureBan.getTime(),
      bannedUntil: futureBan,
      updatedAt: ts,
    })
  );

  // Banned Alice cannot post to Exam Chat or Direct Chat
  await assertFails(
    aliceDb.collection("exam_chats").doc("NEET").collection("messages").doc("msg_2").set({
      senderId: ALICE_UID,
      senderName: "Aarav S",
      examId: "NEET",
      threadId: "",
      participantIds: [],
      text: "Hello again",
      attachmentLabel: "",
      attachmentType: "NONE",
      createdAt: nowTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("direct_chat_threads").doc(threadId).update({
      participantIds: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Aarav", [BOB_UID]: "Bob" },
      lastMessage: "Bypass attempt",
      lastSenderId: ALICE_UID,
      updatedAt: nowTimestamp(),
    })
  );
  await assertFails(
    aliceDb.collection("direct_chats").doc(threadId).collection("messages").doc("dm_2").set({
      senderId: ALICE_UID,
      senderName: "Aarav",
      examId: "NEET",
      threadId,
      participantIds: [ALICE_UID, BOB_UID],
      text: "Bypass attempt",
      attachmentLabel: "",
      attachmentType: "NONE",
      createdAt: nowTimestamp(),
    })
  );

  // Alice cannot delete or shorten her active ban
  await assertFails(aliceDb.collection("chat_moderation").doc(ALICE_UID).delete());
  await assertFails(
    aliceDb.collection("chat_moderation").doc(ALICE_UID).set({
      userId: ALICE_UID,
      warningCount: 0,
      lastWarningReason: "",
      bannedUntilEpochMs: 0,
      bannedUntil: new Date(0),
      updatedAt: nowTimestamp(),
    })
  );
});

