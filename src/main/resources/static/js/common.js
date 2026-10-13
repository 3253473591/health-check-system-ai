const API_BASE = '/api';

function showToast(msg, type = 'info', onConfirm = null) {
    const icons = { success: '✅', error: '❌', warn: '⚠️', info: 'ℹ️' };
    const titles = { success: '成功', error: '错误', warn: '警告', info: '提示' };
    const icon = icons[type] || icons.info;
    const title = titles[type] || titles.info;

    let overlay = document.getElementById('toast-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'toast-overlay';
        overlay.className = 'msg-overlay';
        document.body.appendChild(overlay);
    }

    overlay.innerHTML = `
        <div class="msg-box msg-${type}">
            <div class="msg-icon">${icon}</div>
            <div class="msg-title">${title}</div>
            <div class="msg-content">${msg}</div>
            <div class="msg-btns">
                <button class="btn btn-primary" id="toast-ok">确认</button>
            </div>
        </div>
    `;

    overlay.classList.add('active');

    const close = () => {
        overlay.classList.remove('active');
        if (onConfirm) onConfirm();
    };

    document.getElementById('toast-ok').onclick = close;
    overlay.onclick = e => { if (e.target === overlay) close(); };
}

function showConfirm(msg, title = '请确认', icon = '❓') {
    return new Promise(resolve => {
        let overlay = document.getElementById('confirm-overlay');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'confirm-overlay';
            overlay.className = 'msg-overlay';
            document.body.appendChild(overlay);
        }

        overlay.innerHTML = `
            <div class="msg-box">
                <div class="msg-icon">${icon}</div>
                <div class="msg-title">${title}</div>
                <div class="msg-content">${msg}</div>
                <div class="msg-btns">
                    <button class="btn btn-secondary" id="confirm-cancel">取消</button>
                    <button class="btn btn-primary" id="confirm-ok">确认</button>
                </div>
            </div>
        `;

        overlay.classList.add('active');

        const close = val => {
            overlay.classList.remove('active');
            resolve(val);
        };

        document.getElementById('confirm-ok').onclick = () => close(true);
        document.getElementById('confirm-cancel').onclick = () => close(false);
        overlay.onclick = e => { if (e.target === overlay) close(false); };
    });
}

async function request(url, method, data) {
    const token = localStorage.getItem('token');
    const options = {
        method,
        headers: { 'Content-Type': 'application/json' }
    };
    if (token) options.headers['Authorization'] = token;
    if (data && method !== 'GET') options.body = JSON.stringify(data);

    try {
        const response = await fetch(API_BASE + url, options);
        const result   = await response.json();
        if (result.code === 200) return result.data;

        if (result.code === 403) {
            showToast('权限不足: ' + result.msg, 'error');
            return null;
        }
        showToast('操作失败: ' + result.msg, 'error');
        if (response.status === 401 || result.msg.includes('登录')) {
            localStorage.clear();
            window.location.href = '/login.html';
        }
        return null;
    } catch (e) {
        console.error(e);
        showToast('网络错误，请检查连接', 'error');
        return null;
    }
}

async function requestImport(url, formData) {
    const token = localStorage.getItem('token');
    try {
        const response = await fetch(API_BASE + url, {
            method: 'POST',
            headers: { 'Authorization': token || '' },
            body: formData
        });
        return await response.json();
    } catch (e) {
        console.error(e);
        showToast('网络错误', 'error');
        return null;
    }
}

function logout() {
    localStorage.clear();
    window.location.href = '/login.html';
}

function checkAuth(requiredRole) {
    const token = localStorage.getItem('token');
    const role  = localStorage.getItem('role');
    if (!token) { window.location.href = '/login.html'; return; }
    if (requiredRole && role !== requiredRole) {
        showToast('无权限访问该页面', 'error');
        if (role === 'ADMIN')       window.location.href = '/user.html';
        else if (role === 'DOCTOR') window.location.href = '/doctor-home.html';
        else if (role === 'HR')     window.location.href = '/hr-home.html';
        else window.location.href = '/login.html';
    }
}

function renderPagination(selector, currentPage, totalPages, callback) {
    const container = document.querySelector(selector);
    if (!container) return;
    container.innerHTML = '';

    const prevBtn = document.createElement('button');
    prevBtn.innerText = '上一页';
    prevBtn.disabled  = currentPage === 1;
    prevBtn.onclick   = () => callback(currentPage - 1);
    container.appendChild(prevBtn);

    const info = document.createElement('span');
    info.style.margin = '0 10px';
    info.innerText    = `第 ${currentPage} / ${totalPages || 1} 页`;
    container.appendChild(info);

    const nextBtn = document.createElement('button');
    nextBtn.innerText = '下一页';
    nextBtn.disabled  = currentPage >= totalPages;
    nextBtn.onclick   = () => callback(currentPage + 1);
    container.appendChild(nextBtn);
}

function maskSensitive(val) {
    if (!val || val.length <= 7) return val || '-';
    return val.slice(0, 3) + '*'.repeat(val.length - 7) + val.slice(-4);
}