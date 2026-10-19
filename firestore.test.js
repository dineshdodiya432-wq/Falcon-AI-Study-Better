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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validProfile(uid) {
  const ts = new Date(Date.now() - 1000);
  return {
    userId: uid,
    displayName: "Cosmic Explorer",
    selectedGrade: 6,
    selectedLanguage: "Simple English",
    curriculumTags: ["Algebra", "Life Science"],
    coins: 150,
    xp: 300,
    studyStreak: 4,
    quizStreak: 3,
    brokenStudyStreakToRecover: 0,
    brokenQuizStreakToRecover: 0,
    streakShields: 1,
    unlockedBadges: ["first_launch"],
    reminderEnabled: true,
    reminderHour: 17,
    reminderMinute: 30,
    reminderDays: ["Mon", "Tue", "Wed", "Thu", "Fri"],
    lastStudyDate: "2026-10-19",
    lastQuizDate: "2026-10-19",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validHomework(uid) {
  const ts = new Date(Date.now() - 1000);
  return {
    userId: uid,
    questionTitle: "How do plants make food?",
    subjectTag: "Life Science",
    gradeLevel: 5,
    language: "Simple English",
    steps: ["Step 1: Leaves absorb sunlight.", "Step 2: Roots drink water."],
    finalAnswer: "Plants use photosynthesis to turn sunlight, water, and CO2 into glucose.",
    keyConcept: "Photosynthesis (Chlorophyll & Sunlight)",
    practiceQuestion: "What gas do plants take in during photosynthesis?",
    practiceOptions: ["Carbon Dioxide", "Oxygen", "Helium", "Nitrogen"],
    practiceCorrectIndex: 0,
    practiceCompleted: false,
    coinsEarned: 20,
    createdAt: ts,
    updatedAt: ts,
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

test("Unauthenticated user cannot read or create profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
});

test("Authenticated user can create and read their own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user cannot read or write another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ coins: 9999 }));
});

test("Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      coins: 200,
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Authenticated user can create and query their own homework sessions", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("homework").doc("hw_1").set(validHomework(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("homework").where("userId", "==", ALICE_UID).get()
  );
});

test("Cross-user homework query is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("homework").doc("hw_1").set(validHomework(ALICE_UID))
  );
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("homework").where("userId", "==", ALICE_UID).get()
  );
});
