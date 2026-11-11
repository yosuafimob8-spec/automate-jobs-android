import requests
import gmail_oauth_client as gmail

def send_application(account_id, payload):
    if type(account_id) is not int or account_id <= 0:
        raise ValueError("ID akun KarirMate tidak valid")

    data = dict(payload)
    data.pop("telegram_user_id", None)
    data["account_id"] = str(account_id)

    with requests.post(
        gmail.BASE + "/api/karirmate/gmail/send-application",
        json=data,
        headers={
            **gmail.headers(),
            "Idempotency-Key": data["job_key"]
        },
        timeout=(10, 55),
        allow_redirects=False
    ) as response:
        result = gmail._json(response)
        message_id = result.get("message_id")

        if (
            response.status_code != 200
            or result.get("success") is not True
            or not isinstance(message_id, str)
            or not message_id.strip()
        ):
            raise gmail.GmailUnavailable(
                "Pengiriman belum dikonfirmasi Gmail"
            )

        return message_id
