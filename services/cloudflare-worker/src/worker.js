const SCOPE = "https://www.googleapis.com/auth/gmail.send";
const CALLBACK = "/oauth/google/callback";
function reply(message, status = 200) {
  return new Response(message, {
    status,
    headers: {
      "Content-Type": "text/plain; charset=utf-8",
      "Cache-Control": "no-store",
      "Referrer-Policy": "no-referrer",
      "X-Content-Type-Options": "nosniff",
    },
  });
}
function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
    },
  });
}
function base64url(bytes) {
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}
function randomToken() {
  return base64url(crypto.getRandomValues(new Uint8Array(32)));
}
async function hash(value) {
  const bytes = new TextEncoder().encode(value);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return base64url(new Uint8Array(digest));
}
async function authorized(request, env) {
  const header = request.headers.get("Authorization") || "";
  const supplied = header.startsWith("Bearer ") ? header.slice(7) : "";
  if (!supplied || !env.BOT_API_KEY) return false;
  return (await hash(supplied)) === (await hash(env.BOT_API_KEY));
}
async function encryptionKey(env) {
  const decoded = Uint8Array.from(atob(env.TOKEN_ENCRYPTION_KEY.trim()), (c) =>
    c.charCodeAt(0)
  );
  if (decoded.length !== 32) {
    throw new Error("Encryption key must contain 32 bytes");
  }
  return crypto.subtle.importKey("raw", decoded, "AES-GCM", false, [
    "encrypt",
    "decrypt",
  ]);
}
async function encryptToken(token, env) {
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const key = await encryptionKey(env);
  const encrypted = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv },
    key,
    new TextEncoder().encode(token)
  );
  return "v1:" + base64url(iv) + ":" + base64url(new Uint8Array(encrypted));
}
function decodeBase64url(input) {
  const base64 = input.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, "=");
  return Uint8Array.from(atob(padded), (c) => c.charCodeAt(0));
}
async function decryptGmailToken(value, env) {
  const [version, ivText, ciphertext] = value.split(":");
  if (version !== "v1" || !ivText || !ciphertext) {
    throw new Error("Unsupported token format");
  }
  const key = await encryptionKey(env);
  const plaintext = await crypto.subtle.decrypt(
    { name: "AES-GCM", iv: decodeBase64url(ivText) },
    key,
    decodeBase64url(ciphertext)
  );
  return new TextDecoder().decode(plaintext);
}
async function sendGmailPdfTest(env, uid, pdfBase64) {
  const recipient = String(env.TEST_RECIPIENT || "").trim();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(recipient)) {
    return { error: "invalid_test_recipient", status: 400 };
  }
  if (
    typeof pdfBase64 !== "string" ||
    pdfBase64.length > 3_000_000 ||
    !/^[A-Za-z0-9+/]+={0,2}$/.test(pdfBase64)
  ) {
    return { error: "invalid_pdf_data", status: 400 };
  }
  let pdf;
  try {
    pdf = Uint8Array.from(atob(pdfBase64), (c) => c.charCodeAt(0));
  } catch {
    return { error: "invalid_pdf_encoding", status: 400 };
  }
  if (pdf.length < 5 || new TextDecoder().decode(pdf.slice(0, 5)) !== "%PDF-") {
    return { error: "not_a_pdf", status: 400 };
  }
  const connection = await env.DB.prepare(
    `SELECT encrypted_refresh_token
     FROM gmail_connections WHERE telegram_user_id=?`
  )
    .bind(uid)
    .first();
  if (!connection) {
    return { error: "gmail_not_connected", status: 404 };
  }
  const refreshToken = await decryptGmailToken(
    connection.encrypted_refresh_token,
    env
  );
  const refreshed = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams({
      client_id: env.GOOGLE_CLIENT_ID,
      client_secret: env.GOOGLE_CLIENT_SECRET,
      refresh_token: refreshToken,
      grant_type: "refresh_token",
    }),
  });
  if (!refreshed.ok) {
    return { error: "gmail_refresh_failed", status: 400 };
  }
  const { access_token } = await refreshed.json();
  const boundary = "automate_" + crypto.randomUUID().replace(/-/g, "");
  const body = [
    "Yth. Tim Rekrutmen,",
    "",
    "Berikut saya lampirkan CV untuk pengujian",
    "fitur pengiriman lamaran AutoMateSmartBot.",
    "",
    "Email ini hanya untuk pengujian sistem.",
    "Tidak ditujukan sebagai lamaran kerja sungguhan.",
  ].join("\r\n");
  const wrappedPdf = pdfBase64.match(/.{1,76}/g).join("\r\n");
  const mime = [
    `To: ${recipient}`,
    "Subject: TEST - Lamaran AutoMateSmartBot",
    "MIME-Version: 1.0",
    `Content-Type: multipart/mixed; boundary="${boundary}"`,
    "",
    `--${boundary}`,
    "Content-Type: text/plain; charset=UTF-8",
    "",
    body,
    "",
    `--${boundary}`,
    'Content-Type: application/pdf; name="CV.pdf"',
    'Content-Disposition: attachment; filename="CV.pdf"',
    "Content-Transfer-Encoding: base64",
    "",
    wrappedPdf,
    "",
    `--${boundary}--`,
    "",
  ].join("\r\n");
  const bytes = new TextEncoder().encode(mime);
  let binary = "";
  for (let i = 0; i < bytes.length; i += 8192) {
    binary += String.fromCharCode(...bytes.subarray(i, i + 8192));
  }
  const raw = btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
  const sent = await fetch(
    "https://gmail.googleapis.com/gmail/v1/users/me/messages/send",
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${access_token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ raw }),
    }
  );
  if (!sent.ok) {
    return { error: "gmail_send_failed", status: sent.status };
  }
  const result = await sent.json();
  return {
    success: true,
    message_id: result.id,
    status: 200,
  };
}
async function sendGmailTest(env, uid) {
  const connection = await env.DB.prepare(
    `SELECT encrypted_refresh_token
     FROM gmail_connections
     WHERE telegram_user_id = ?`
  )
    .bind(uid)
    .first();
  if (!connection) {
    return { error: "gmail_not_connected", status: 404 };
  }
  const recipient = String(env.TEST_RECIPIENT || "").trim();
  if (
    !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(recipient) ||
    /[\r\n]/.test(recipient)
  ) {
    return { error: "invalid_test_recipient", status: 400 };
  }
  const refreshToken = await decryptGmailToken(
    connection.encrypted_refresh_token,
    env
  );
  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams({
      client_id: env.GOOGLE_CLIENT_ID,
      client_secret: env.GOOGLE_CLIENT_SECRET,
      refresh_token: refreshToken,
      grant_type: "refresh_token",
    }),
  });
  if (!tokenResponse.ok) {
    const problem = await tokenResponse.json().catch(() => ({}));
    console.error("Gmail refresh failed:", String(problem.error || "unknown"));
    return { error: "gmail_refresh_failed", status: 400 };
  }
  const token = await tokenResponse.json();
  const message = [
    `To: ${recipient}`,
    "Subject: AutoMateSmartBot - Tes Gmail",
    "MIME-Version: 1.0",
    "Content-Type: text/plain; charset=UTF-8",
    "",
    "Tes pengiriman Gmail AutoMateSmartBot berhasil.",
    "Ini bukan email lamaran pekerjaan.",
  ].join("\r\n");
  const raw = base64url(new TextEncoder().encode(message));
  const sendResponse = await fetch(
    "https://gmail.googleapis.com/gmail/v1/users/me/messages/send",
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token.access_token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ raw }),
    }
  );
  if (!sendResponse.ok) {
    console.error("Gmail send HTTP:", sendResponse.status);
    return {
      error: "gmail_send_failed",
      status: sendResponse.status,
    };
  }
  const sent = await sendResponse.json();
  return {
    success: true,
    message_id: sent.id,
    status: 200,
  };
}
// Fase 5.12 — send production applications. Backend authentication required.
const BLOCKED_MAIL_DOMAINS = [
  "kalibrr.com",
  "glints.com",
  "indeed.com",
  "dealls.com",
  "jobstreet.com",
  "linkedin.com",
];
function looksLikeEmail(value) {
  return (
    typeof value === "string" &&
    value.length <= 254 &&
    /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(
      value
    ) &&
    !/[\r\n]/.test(value)
  );
}
function safeHeaderText(value, max = 120) {
  const v = String(value || "").trim();
  if (!v || v.length > max || /[\r\n\x00-\x1f\x7f]/.test(v))
    throw new Error("invalid_header");
  return v;
}
function encodedSubject(value) {
  const encoded = btoa(String.fromCharCode(...new TextEncoder().encode(value)));
  return "=?UTF-8?B?" + encoded + "?=";
}
function encodeMail(text) {
  const data = new TextEncoder().encode(text);
  let binary = "";
  for (let p = 0; p < data.length; p += 8192) {
    binary += String.fromCharCode(...data.subarray(p, p + 8192));
  }
  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}
async function sendApplication(request, env) {
  if (!(await authorized(request, env)))
    return json({ error: "Unauthorized" }, 401);
  const b = await request.json().catch(() => ({}));
  const uid = String(b.telegram_user_id || "");
  const to = String(b.to || "")
    .trim()
    .toLowerCase();
  const key = String(b.job_key || "");
  if (
    !/^[1-9][0-9]{4,19}$/.test(uid) ||
    !looksLikeEmail(to) ||
    !/^[a-f0-9]{64}$/.test(key)
  ) {
    return json({ error: "invalid_request" }, 400);
  }
  const domain = to.split("@")[1];
  if (
    BLOCKED_MAIL_DOMAINS.some((d) => domain === d || domain.endsWith("." + d))
  ) {
    return json({ error: "portal_email_blocked" }, 400);
  }
  let person, position, company;
  try {
    person = safeHeaderText(b.applicant_name, 100);
    position = safeHeaderText(b.job_title, 130);
    company = safeHeaderText(b.company, 100);
  } catch {
    return json({ error: "invalid_message_fields" }, 400);
  }
  if (
    typeof b.pdf_base64 !== "string" ||
    b.pdf_base64.length > 3_000_000 ||
    !/^[A-Za-z0-9+/]+={0,2}$/.test(b.pdf_base64)
  )
    return json({ error: "invalid_pdf_data" }, 400);
  let pdf;
  try {
    pdf = atob(b.pdf_base64);
  } catch {
    return json({ error: "invalid_pdf_encoding" }, 400);
  }
  if (!pdf.startsWith("%PDF-")) return json({ error: "invalid_pdf" }, 400);
  const connection = await env.DB.prepare(
    "SELECT encrypted_refresh_token FROM gmail_connections WHERE telegram_user_id=?"
  )
    .bind(uid)
    .first();
  if (!connection) return json({ error: "gmail_not_connected" }, 404);
  await env.DB.prepare(
    `CREATE TABLE IF NOT EXISTS application_sends (
     telegram_user_id TEXT NOT NULL, job_key TEXT NOT NULL,
     recipient TEXT NOT NULL, state TEXT NOT NULL,
     gmail_message_id TEXT, reserved_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY(telegram_user_id,job_key)
  )`
  ).run();
  // Reserve BEFORE external Gmail call. Ambiguous errors stay reserved; never auto-resend.
  const reservation = await env.DB.prepare(
    "INSERT OR IGNORE INTO application_sends (telegram_user_id,job_key,recipient,state) VALUES (?,?,?,?)"
  )
    .bind(uid, key, to, "reserved")
    .run();
  if (reservation.meta.changes !== 1) {
    const existing = await env.DB.prepare(
      "SELECT state,gmail_message_id FROM application_sends WHERE telegram_user_id=? AND job_key=?"
    )
      .bind(uid, key)
      .first();
    return json(
      {
        error: "already_reserved",
        state: existing?.state || "unknown",
        message_id: existing?.gmail_message_id || null,
      },
      409
    );
  }
  // No further automatic retry after reserving, including refresh errors.
  try {
    const refreshToken = await decryptGmailToken(
      connection.encrypted_refresh_token,
      env
    );
    const refreshed = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        client_id: env.GOOGLE_CLIENT_ID,
        client_secret: env.GOOGLE_CLIENT_SECRET,
        refresh_token: refreshToken,
        grant_type: "refresh_token",
      }),
    });
    if (!refreshed.ok) throw new Error("gmail_refresh_failed");
    const { access_token } = await refreshed.json();
    const subject = encodedSubject(`Lamaran Kerja - ${position} - ${person}`);
    const body = [
      "Yth. Tim Rekrutmen " + company + ",",
      "",
      "Dengan hormat,",
      "",
      `Saya ${person} bermaksud mengajukan lamaran untuk posisi ${position} di ${company}.`,
      "Bersama email ini saya lampirkan CV untuk bahan pertimbangan.",
      "",
      "Terima kasih atas waktu dan perhatian Bapak/Ibu.",
      "",
      "Hormat saya,",
      person,
    ].join("\r\n");
    const boundary = "cv_" + crypto.randomUUID().replace(/-/g, "");
    const wrappedPdf = b.pdf_base64.match(/.{1,76}/g).join("\r\n");
    const message = [
      `To: ${to}`,
      `Subject: ${subject}`,
      "MIME-Version: 1.0",
      `Content-Type: multipart/mixed; boundary="${boundary}"`,
      "",
      `--${boundary}`,
      "Content-Type: text/plain; charset=UTF-8",
      "Content-Transfer-Encoding: base64",
      "",
      btoa(String.fromCharCode(...new TextEncoder().encode(body))),
      "",
      `--${boundary}`,
      'Content-Type: application/pdf; name="CV.pdf"',
      'Content-Disposition: attachment; filename="CV.pdf"',
      "Content-Transfer-Encoding: base64",
      "",
      wrappedPdf,
      "",
      `--${boundary}--`,
      "",
    ].join("\r\n");
    const res = await fetch(
      "https://gmail.googleapis.com/gmail/v1/users/me/messages/send",
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${access_token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ raw: encodeMail(message) }),
      }
    );
    if (!res.ok) throw new Error("gmail_http_" + res.status);
    const result = await res.json();
    await env.DB.prepare(
      "UPDATE application_sends SET state=?,gmail_message_id=? WHERE telegram_user_id=? AND job_key=?"
    )
      .bind("sent", result.id, uid, key)
      .run();
    return json({ success: true, message_id: result.id }, 200);
  } catch (err) {
    console.error("Application send state uncertain:", err.name);
    await env.DB.prepare(
      "UPDATE application_sends SET state=? WHERE telegram_user_id=? AND job_key=?"
    )
      .bind("uncertain", uid, key)
      .run();
    return json({ error: "send_uncertain_check_gmail_before_retry" }, 502);
  }
}

// KARIRMATE_GMAIL_SEND_V2
async function sendKarirMateApplication(request, env) {
  if (!(await authorized(request, env)))
    return json({ error: "Unauthorized" }, 401);
  const b = await request.json().catch(() => ({}));
  const uid = karirMateAccountId(b.account_id);
  const to = String(b.to || "")
    .trim()
    .toLowerCase();
  const key = String(b.job_key || "");
  if (
    !uid ||
    !looksLikeEmail(to) ||
    !/^[a-f0-9]{64}$/.test(key)
  ) {
    return json({ error: "invalid_request" }, 400);
  }
  const domain = to.split("@")[1];
  if (
    BLOCKED_MAIL_DOMAINS.some((d) => domain === d || domain.endsWith("." + d))
  ) {
    return json({ error: "portal_email_blocked" }, 400);
  }
  let person, position, company;
  try {
    person = safeHeaderText(b.applicant_name, 100);
    position = safeHeaderText(b.job_title, 130);
    company = safeHeaderText(b.company, 100);
  } catch {
    return json({ error: "invalid_message_fields" }, 400);
  }
  if (
    typeof b.pdf_base64 !== "string" ||
    b.pdf_base64.length > 3_000_000 ||
    !/^[A-Za-z0-9+/]+={0,2}$/.test(b.pdf_base64)
  )
    return json({ error: "invalid_pdf_data" }, 400);
  let pdf;
  try {
    pdf = atob(b.pdf_base64);
  } catch {
    return json({ error: "invalid_pdf_encoding" }, 400);
  }
  if (!pdf.startsWith("%PDF-")) return json({ error: "invalid_pdf" }, 400);
  const connection = await env.DB.prepare(
    "SELECT encrypted_refresh_token FROM km_gmail_connections WHERE account_id=?"
  )
    .bind(uid)
    .first();
  if (!connection) return json({ error: "gmail_not_connected" }, 404);
  await env.DB.prepare(
    `CREATE TABLE IF NOT EXISTS km_application_sends (
     account_id TEXT NOT NULL, job_key TEXT NOT NULL,
     recipient TEXT NOT NULL, state TEXT NOT NULL,
     gmail_message_id TEXT, reserved_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY(account_id,job_key)
  )`
  ).run();
  // Reserve BEFORE external Gmail call. Ambiguous errors stay reserved; never auto-resend.
  const reservation = await env.DB.prepare(
    "INSERT OR IGNORE INTO km_application_sends (account_id,job_key,recipient,state) VALUES (?,?,?,?)"
  )
    .bind(uid, key, to, "reserved")
    .run();
  if (reservation.meta.changes !== 1) {
    const existing = await env.DB.prepare(
      "SELECT state,gmail_message_id FROM km_application_sends WHERE account_id=? AND job_key=?"
    )
      .bind(uid, key)
      .first();
    return json(
      {
        error: "already_reserved",
        state: existing?.state || "unknown",
        message_id: existing?.gmail_message_id || null,
      },
      409
    );
  }
  // No further automatic retry after reserving, including refresh errors.
  try {
    const refreshToken = await decryptGmailToken(
      connection.encrypted_refresh_token,
      env
    );
    const refreshed = await fetch("https://oauth2.googleapis.com/token", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        client_id: env.KARIRMATE_GOOGLE_CLIENT_ID,
        client_secret: env.KARIRMATE_GOOGLE_CLIENT_SECRET,
        refresh_token: refreshToken,
        grant_type: "refresh_token",
      }),
    });
    if (!refreshed.ok) throw new Error("gmail_refresh_failed");
    const { access_token } = await refreshed.json();
    const requestedSubject =
      typeof b.subject === "string" ? b.subject : "";
    const subjectText = requestedSubject.trim();
    const safeSubject =
      subjectText &&
      subjectText.length <= 180 &&
      !/[\r\n]/.test(requestedSubject)
        ? subjectText
        : `Lamaran Kerja - ${position} - ${person}`;

    const subject = encodedSubject(safeSubject);

    const requestedBody =
      typeof b.body === "string" ? b.body.trim() : "";

    const body = (
      requestedBody && requestedBody.length <= 12000
        ? requestedBody
        : [
            "Yth. Tim Rekrutmen " + company + ",",
            "",
            "Dengan hormat,",
            "",
            `Saya ${person} bermaksud mengajukan lamaran untuk posisi ${position} di ${company}.`,
            "Bersama email ini saya lampirkan CV untuk bahan pertimbangan.",
            "",
            "Terima kasih atas waktu dan perhatian Bapak/Ibu.",
            "",
            "Hormat saya,",
            person,
          ].join("\r\n")
    ).replace(/\r\n|\r|\n/g, "\r\n");
    const boundary = "cv_" + crypto.randomUUID().replace(/-/g, "");
    const wrappedPdf = b.pdf_base64.match(/.{1,76}/g).join("\r\n");
    const message = [
      `To: ${to}`,
      `Subject: ${subject}`,
      "MIME-Version: 1.0",
      `Content-Type: multipart/mixed; boundary="${boundary}"`,
      "",
      `--${boundary}`,
      "Content-Type: text/plain; charset=UTF-8",
      "Content-Transfer-Encoding: base64",
      "",
      btoa(String.fromCharCode(...new TextEncoder().encode(body))),
      "",
      `--${boundary}`,
      'Content-Type: application/pdf; name="CV.pdf"',
      'Content-Disposition: attachment; filename="CV.pdf"',
      "Content-Transfer-Encoding: base64",
      "",
      wrappedPdf,
      "",
      `--${boundary}--`,
      "",
    ].join("\r\n");
    const res = await fetch(
      "https://gmail.googleapis.com/gmail/v1/users/me/messages/send",
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${access_token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ raw: encodeMail(message) }),
      }
    );
    if (!res.ok) {
      const googleResponse = await res.json().catch(() => ({}));
      const googleError = googleResponse.error || {};

      console.error("Gmail API rejection:", {
        httpStatus: res.status,
        googleStatus: googleError.status || null,
        reason:
          googleError.errors?.[0]?.reason ||
          googleError.details?.[0]?.reason ||
          null,
      });

      throw new Error("gmail_http_" + res.status);
    }
    const result = await res.json();
    await env.DB.prepare(
      "UPDATE km_application_sends SET state=?,gmail_message_id=? WHERE account_id=? AND job_key=?"
    )
      .bind("sent", result.id, uid, key)
      .run();
    return json({ success: true, message_id: result.id }, 200);
  } catch (err) {
    console.error("Application send state uncertain:", err.name);
    await env.DB.prepare(
      "UPDATE km_application_sends SET state=? WHERE account_id=? AND job_key=?"
    )
      .bind("uncertain", uid, key)
      .run();
    return json({ error: "send_uncertain_check_gmail_before_retry" }, 502);
  }
}

// KarirMate identity is account_id from an authenticated Termux backend session.
// Separate tables deliberately preserve legacy Telegram OAuth and Gmail tokens.
function karirMateAccountId(value) {
  const raw = String(value ?? "");
  if (!/^[1-9][0-9]{0,17}$/.test(raw)) return null;
  const id = Number(raw);
  return Number.isSafeInteger(id) && String(id) === raw ? raw : null;
}

async function ensureKarirMateTables(env) {
  await env.DB.prepare(
    `CREATE TABLE IF NOT EXISTS km_oauth_sessions (
    state_hash TEXT PRIMARY KEY,
    account_id INTEGER NOT NULL,
    expires_at INTEGER NOT NULL
  )`
  ).run();
  await env.DB.prepare(
    `CREATE TABLE IF NOT EXISTS km_gmail_connections (
    account_id INTEGER PRIMARY KEY,
    encrypted_refresh_token TEXT NOT NULL,
    connected_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    auto_apply_enabled INTEGER NOT NULL DEFAULT 0
  )`
  ).run();
}

async function startKarirMateOAuth(request, env, url, now) {
  if (!(await authorized(request, env)))
    return json({ error: "Unauthorized" }, 401);
  const body = await request.json().catch(() => ({}));
  const accountId = karirMateAccountId(body.account_id);
  if (!accountId) return json({ error: "Invalid KarirMate account ID" }, 400);
  await ensureKarirMateTables(env);
  const state = randomToken();
  const stateHash = await hash(state);
  await env.DB.prepare(
    `INSERT INTO km_oauth_sessions
    (state_hash, account_id, expires_at) VALUES (?, ?, ?)`
  )
    .bind(stateHash, accountId, now + 600)
    .run();
  const google = new URL("https://accounts.google.com/o/oauth2/v2/auth");
  google.searchParams.set("client_id", env.KARIRMATE_GOOGLE_CLIENT_ID);
  google.searchParams.set("redirect_uri", url.origin + CALLBACK);
  google.searchParams.set("response_type", "code");
  google.searchParams.set("scope", SCOPE);
  google.searchParams.set("access_type", "offline");
  google.searchParams.set("prompt", "consent");
  google.searchParams.set("state", state);
  return json({ url: google.toString(), expires_in: 600 });
}

async function statusKarirMateOAuth(request, env, url) {
  if (!(await authorized(request, env)))
    return json({ error: "Unauthorized" }, 401);
  const accountId = karirMateAccountId(url.searchParams.get("account_id"));
  if (!accountId) return json({ error: "Invalid KarirMate account ID" }, 400);
  await ensureKarirMateTables(env);
  const row = await env.DB.prepare(
    `SELECT connected_at
    FROM km_gmail_connections WHERE account_id=?`
  )
    .bind(accountId)
    .first();
  return json({
    account_id: accountId,
    connected: Boolean(row),
    connected_at: row?.connected_at || null,
    auto_apply_enabled: false,
  });
}

async function completeKarirMateOAuth(stateHash, code, env, url, now) {
  await ensureKarirMateTables(env);
  const session = await env.DB.prepare(
    `SELECT account_id, expires_at
    FROM km_oauth_sessions WHERE state_hash=?`
  )
    .bind(stateHash)
    .first();
  if (!session) return null; // This OAuth state belongs to the legacy Telegram flow.
  const removed = await env.DB.prepare(
    `DELETE FROM km_oauth_sessions
    WHERE state_hash=? AND expires_at>=?`
  )
    .bind(stateHash, now)
    .run();
  if (removed.meta.changes !== 1)
    return reply("Sesi Gmail kedaluwarsa atau sudah dipakai.", 400);
  if (!code) return reply("Izin Gmail dibatalkan.", 400);
  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      code,
      client_id: env.KARIRMATE_GOOGLE_CLIENT_ID,
      client_secret: env.KARIRMATE_GOOGLE_CLIENT_SECRET,
      redirect_uri: url.origin + CALLBACK,
      grant_type: "authorization_code",
    }),
  });
  if (!tokenResponse.ok)
    return reply("Google tidak menyelesaikan otorisasi Gmail.", 400);
  const tokenData = await tokenResponse.json();
  if (
    !String(tokenData.scope || "")
      .split(/\s+/)
      .includes(SCOPE)
  ) {
    return reply("Izin gmail.send tidak diberikan.", 403);
  }
  if (!tokenData.refresh_token)
    return reply("Refresh token Gmail tidak diberikan.", 400);
  const encrypted = await encryptToken(tokenData.refresh_token, env);
  await env.DB.prepare(
    `INSERT INTO km_gmail_connections
    (account_id, encrypted_refresh_token, connected_at, auto_apply_enabled)
    VALUES (?, ?, CURRENT_TIMESTAMP, 0)
    ON CONFLICT(account_id) DO UPDATE SET
      encrypted_refresh_token=excluded.encrypted_refresh_token,
      connected_at=CURRENT_TIMESTAMP,
      auto_apply_enabled=0`
  )
    .bind(session.account_id, encrypted)
    .run();
  return reply(
    "Gmail berhasil terhubung ke akun KarirMate. Kembali ke aplikasi, lalu tekan Muat Ulang. Pengiriman otomatis tetap nonaktif."
  );
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;
    const now = Math.floor(Date.now() / 1000);
    try {
      if (path === "/" && request.method === "GET") {
        return reply("AutoMateSmartBot Gmail OAuth aktif");
      }
      // Hanya backend bot yang boleh membuat sesi OAuth
      if (path === "/api/karirmate/oauth/start" && request.method === "POST") {
        return startKarirMateOAuth(request, env, url, now);
      }
      if (path === "/api/karirmate/oauth/status" && request.method === "GET") {
        return statusKarirMateOAuth(request, env, url);
      }
      if (path === "/api/oauth/start" && request.method === "POST") {
        if (!(await authorized(request, env))) {
          return json({ error: "Unauthorized" }, 401);
        }
        const body = await request.json();
        const uid = String(body.telegram_user_id || "");
        if (!/^[1-9][0-9]{4,19}$/.test(uid)) {
          return json({ error: "Invalid Telegram user ID" }, 400);
        }
        const state = randomToken();
        const stateHash = await hash(state);
        await env.DB.prepare(
          `INSERT INTO oauth_sessions
           (state_hash, telegram_user_id, expires_at)
           VALUES (?, ?, ?)`
        )
          .bind(stateHash, uid, now + 600)
          .run();
        const google = new URL("https://accounts.google.com/o/oauth2/v2/auth");
        google.searchParams.set("client_id", env.GOOGLE_CLIENT_ID);
        google.searchParams.set("redirect_uri", url.origin + CALLBACK);
        google.searchParams.set("response_type", "code");
        google.searchParams.set("scope", SCOPE);
        google.searchParams.set("access_type", "offline");
        google.searchParams.set("prompt", "consent");
        google.searchParams.set("state", state);
        return json({
          url: google.toString(),
          expires_in: 600,
        });
      }
      if (path === CALLBACK && request.method === "GET") {
        const state = url.searchParams.get("state");
        const code = url.searchParams.get("code");
        if (!state || state.length > 200) {
          return reply("Sesi login tidak valid.", 400);
        }
        const stateHash = await hash(state);
        const kmResult = await completeKarirMateOAuth(
          stateHash,
          code,
          env,
          url,
          now
        );
        if (kmResult !== null) return kmResult;
        const session = await env.DB.prepare(
          `SELECT telegram_user_id, expires_at
           FROM oauth_sessions WHERE state_hash=?`
        )
          .bind(stateHash)
          .first();
        if (!session || Number(session.expires_at) < now) {
          return reply(
            "Sesi login sudah kedaluwarsa. Ulangi dari Telegram.",
            400
          );
        }
        // Hanya boleh digunakan sekali
        const deleted = await env.DB.prepare(
          `DELETE FROM oauth_sessions
           WHERE state_hash=?`
        )
          .bind(stateHash)
          .run();
        if (deleted.meta.changes !== 1) {
          return reply("Sesi login sudah digunakan.", 400);
        }
        if (!code) {
          return reply(
            "Login dibatalkan atau Google tidak memberikan izin.",
            400
          );
        }
        const tokenResponse = await fetch(
          "https://oauth2.googleapis.com/token",
          {
            method: "POST",
            headers: {
              "Content-Type": "application/x-www-form-urlencoded",
            },
            body: new URLSearchParams({
              code,
              client_id: env.GOOGLE_CLIENT_ID,
              client_secret: env.GOOGLE_CLIENT_SECRET,
              redirect_uri: url.origin + CALLBACK,
              grant_type: "authorization_code",
            }),
          }
        );
        if (!tokenResponse.ok) {
          return reply("Koneksi Google gagal. Ulangi dari Telegram.", 400);
        }
        const tokenData = await tokenResponse.json();
        const grantedScopes = String(tokenData.scope || "").split(/\s+/);
        if (!grantedScopes.includes(SCOPE)) {
          return reply("Izin gmail.send tidak diberikan.", 403);
        }
        if (!tokenData.refresh_token) {
          return reply("Refresh token tidak diterima. Ulangi login.", 400);
        }
        const encrypted = await encryptToken(tokenData.refresh_token, env);
        await env.DB.prepare(
          `INSERT INTO gmail_connections
           (telegram_user_id, encrypted_refresh_token,
            connected_at, auto_apply_enabled)
           VALUES (?, ?, CURRENT_TIMESTAMP, 0)
           ON CONFLICT(telegram_user_id)
           DO UPDATE SET
           encrypted_refresh_token=excluded.encrypted_refresh_token,
           connected_at=CURRENT_TIMESTAMP,
           auto_apply_enabled=0`
        )
          .bind(session.telegram_user_id, encrypted)
          .run();
        return reply(
          "✅ Gmail berhasil terhubung ke AutoMateSmartBot.\n" +
            "Kembali ke Telegram untuk melanjutkan.\n" +
            "Pengiriman otomatis belum diaktifkan."
        );
      }
      // Status koneksi hanya bisa dibaca backend bot
      if (path === "/api/oauth/status" && request.method === "GET") {
        if (!(await authorized(request, env))) {
          return json({ error: "Unauthorized" }, 401);
        }
        const uid = url.searchParams.get("uid") || "";
        if (!/^[1-9][0-9]{4,19}$/.test(uid)) {
          return json({ error: "Invalid UID" }, 400);
        }
        const connection = await env.DB.prepare(
          `SELECT telegram_user_id, connected_at,
                  auto_apply_enabled
           FROM gmail_connections
           WHERE telegram_user_id=?`
        )
          .bind(uid)
          .first();
        return json({
          connected: Boolean(connection),
          connected_at: connection?.connected_at || null,
          auto_apply_enabled: Boolean(connection?.auto_apply_enabled),
        });
      }
      // Gmail test only — no automatic job applications
      if (path === "/api/gmail/test-send" && request.method === "POST") {
        if (!(await authorized(request, env))) {
          return json({ error: "Unauthorized" }, 401);
        }
        const body = await request.json().catch(() => ({}));
        const uid = String(body.telegram_user_id || "");
        // Pengujian dibatasi ke akun admin
        if (uid !== "5573781407") {
          return json({ error: "Test access denied" }, 403);
        }
        const result = await sendGmailTest(env, uid);
        const { status, ...response } = result;
        return json(response, status);
      }
      if (path === "/api/gmail/test-pdf" && request.method === "POST") {
        if (!(await authorized(request, env))) {
          return json({ error: "Unauthorized" }, 401);
        }
        const body = await request.json().catch(() => ({}));
        const uid = String(body.telegram_user_id || "");
        if (uid !== "5573781407") {
          return json({ error: "Forbidden" }, 403);
        }
        const result = await sendGmailPdfTest(env, uid, body.pdf_base64);
        const { status, ...response } = result;
        return json(response, status);
      }
      if (path === "/api/karirmate/gmail/send-application" && request.method === "POST") {
        return sendKarirMateApplication(request, env);
      }
      if (path === "/api/gmail/send-application" && request.method === "POST") {
        return sendApplication(request, env);
      }
      return reply("Not Found", 404);
    } catch (error) {
      // Jangan tampilkan credential atau token di error publik
      console.error("OAuth operation failed:", error.name);
      return reply("Internal server error", 500);
    }
  },
};