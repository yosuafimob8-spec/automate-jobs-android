"""Local VoxCPM2 TTS for KarirMate. From backend-contract/: uvicorn voxcpm2_tts_api:app --host 127.0.0.1 --port 8791."""
import io
import os
import threading
from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel, Field

app = FastAPI(title="KarirMate VoxCPM2 local TTS")
_model = None
_lock = threading.Lock()
MODEL_ID = os.getenv("KARIRMATE_VOXCPM_MODEL", "openbmb/VoxCPM2")

class SpeechRequest(BaseModel):
    text: str = Field(min_length=1, max_length=500)
    language: str = "id"

@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_ID, "loaded": _model is not None}

@app.post("/tts")
def tts(req: SpeechRequest):
    global _model
    if req.language not in ("id", "en"):
        raise HTTPException(400, "Unsupported language")
    try:
        with _lock:
            if _model is None:
                from voxcpm import VoxCPM
                _model = VoxCPM.from_pretrained(MODEL_ID, load_denoiser=False, optimize=False, device="cpu")
            import soundfile as sf
            wav = _model.generate(text=req.text, cfg_value=2.0, inference_timesteps=10)
            if isinstance(wav, tuple):
                wav = wav[0]
            if hasattr(wav, "detach"):
                wav = wav.detach().cpu().numpy()
            rate = getattr(_model, "sample_rate", None) or getattr(getattr(_model, "tts_model", None), "sample_rate", None) or 16000
            output = io.BytesIO()
            sf.write(output, wav, int(rate), format="WAV")
            return Response(output.getvalue(), media_type="audio/wav")
    except Exception as exc:
        raise HTTPException(503, "VoxCPM2 unavailable: " + type(exc).__name__) from exc
