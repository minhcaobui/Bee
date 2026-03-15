function switchTab(tabId) {
    document.querySelectorAll('.auth-tab').forEach(t => t.classList.remove('active'));
    event.target.classList.add('active');
    document.querySelectorAll('.auth-form').forEach(f => f.classList.remove('active'));
    document.getElementById('form-' + tabId).classList.add('active');
}

function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    const btn = event.currentTarget;
    if (input.type === 'password') {
        input.type = 'text';
        btn.style.opacity = '1';
    } else {
        input.type = 'password';
        btn.style.opacity = '0.5';
    }
}

document.getElementById('registerForm').addEventListener('submit', function (e) {
    const pw = document.getElementById('reg-pw').value;
    const phone = document.getElementById('reg-phone').value;
    if (!/^0[0-9]{9}$/.test(phone)) {
        e.preventDefault();
        alert("Số điện thoại không hợp lệ! Vui lòng nhập đủ 10 số bắt đầu bằng số 0.");
        return;
    }
    if (pw.length < 8) {
        e.preventDefault();
        alert("Mật khẩu phải có ít nhất 8 ký tự!");
    }
});