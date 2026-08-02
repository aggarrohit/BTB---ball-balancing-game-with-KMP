const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Sends a push notification when a game invite is written to another user's inbox.
 * See database.rules.json for who's allowed to write there (the inviter, once, on creation).
 */
exports.onInviteCreated = functions.database
  .ref("/invites/{toUid}/{inviteId}")
  .onCreate(async (snapshot, context) => {
    const invite = snapshot.val();
    const { toUid, inviteId } = context.params;

    if (!invite || invite.status !== "pending") {
      return null;
    }

    const tokensSnapshot = await admin.database().ref(`/users/${toUid}/fcmTokens`).once("value");
    const tokens = tokensSnapshot.exists() ? Object.keys(tokensSnapshot.val()) : [];
    if (tokens.length === 0) {
      return null;
    }

    const response = await admin.messaging().sendEachForMulticast({
      tokens,
      notification: {
        title: "Game invite",
        body: `${invite.fromUsername} invited you to play Balance The Ball`
      },
      data: {
        type: "game_invite",
        inviteId,
        roomCode: String(invite.roomCode),
        fromUid: String(invite.fromUid),
        fromUsername: String(invite.fromUsername)
      }
    });

    // Prune tokens FCM reports as dead (uninstalled app, revoked, etc.) so future sends don't
    // keep paying the (free, but pointless) cost of trying them.
    const staleTokens = [];
    response.responses.forEach((result, index) => {
      if (!result.success) {
        const code = result.error && result.error.code;
        if (
          code === "messaging/invalid-registration-token" ||
          code === "messaging/registration-token-not-registered"
        ) {
          staleTokens.push(tokens[index]);
        }
      }
    });

    if (staleTokens.length > 0) {
      const updates = {};
      staleTokens.forEach((token) => {
        updates[token] = null;
      });
      await admin.database().ref(`/users/${toUid}/fcmTokens`).update(updates);
    }

    return null;
  });
