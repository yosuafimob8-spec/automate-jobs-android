#!/usr/bin/env python3
"""Reject a placeholder Google button before any KarirMate APK is built."""
from pathlib import Path
import sys


def verify(root: Path):
    pkg = root / 'app/src/main/java/com/example/automatesmartbot'
    button = (pkg/'ui/screens/auth/GoogleSignInButton.kt').read_text(encoding='utf-8')
    login = (pkg/'ui/screens/auth/LoginScreen.kt').read_text(encoding='utf-8')
    register = (pkg/'ui/screens/auth/RegisterScreen.kt').read_text(encoding='utf-8')
    repo = (pkg/'data/repository/AuthRepository.kt').read_text(encoding='utf-8')
    api = (pkg/'data/network/ApiService.kt').read_text(encoding='utf-8')
    vm = (pkg/'ui/viewmodel/AuthViewModel.kt').read_text(encoding='utf-8')
    checks = {
       'Google Credential Manager is used': all(k in button for k in ('GetSignInWithGoogleOption', 'CredentialManager.create', '.getCredential(', 'GoogleIdTokenCredential')),
       'Real token sent to login callback': 'onIdToken(googleToken)' in button,
       'Non-empty Google ID token enforced': 'googleToken.isNotBlank()' in button,
       'Login invokes Google auth': 'GoogleSignInButton(' in login and 'viewModel.loginWithGoogle(it, onLoginSuccess)' in login,
       'Register invokes same Google auth': 'GoogleSignInButton(' in register and 'viewModel.loginWithGoogle(it, onGoogleSuccess)' in register,
       'Repository calls backend': 'loginApi.googleLogin(' in repo and 'secureSession.saveToken(' in repo,
       'Backend interface has real endpoint': '@POST("auth/google")' in api,
       'ViewModel calls backend repository': 'repository.loginWithGoogle(idToken)' in vm,
       'No placeholder present': not any(x in (login+register+button).lower() for x in ('login google belum tersedia','perlu integrasi oauth','google login simulasi','google login demo')),
    }
    for name, ok in checks.items():
        print(('OK  ' if ok else 'FAIL') + name)
    return all(checks.values())


if __name__ == '__main__':
    root = Path(sys.argv[1]) if len(sys.argv)>1 else Path(__file__).resolve().parents[1]
    sys.exit(0 if verify(root) else 1)
