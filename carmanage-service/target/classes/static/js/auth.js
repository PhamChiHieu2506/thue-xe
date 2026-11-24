'use strict';
(() => {
  let accessToken = null, expiresTimer = null, user = null;
  const button = document.getElementById('auth-button');
  const dialog = document.createElement('dialog');
  dialog.className = 'auth-dialog';
  dialog.innerHTML = `<form id="login-form"><h2>Đăng nhập hệ thống</h2>
    <p>Dùng tài khoản trong tệp demo-users.txt trên máy của bạn.</p>
    <label for="auth-username">Tên đăng nhập</label><input id="auth-username" autocomplete="username" required maxlength="60">
    <label for="auth-password">Mật khẩu</label><input id="auth-password" type="password" autocomplete="current-password" required maxlength="200">
    <p id="auth-error" role="alert"></p><button class="button primary" type="submit">Đăng nhập</button>
    <button class="button secondary" type="button" id="auth-cancel">Đóng</button></form>`;
  document.body.append(dialog);
  function clear() {
    accessToken=null; user=null; clearTimeout(expiresTimer);button.textContent='Đăng nhập';
    window.dispatchEvent(new Event('auth:changed'));
  }
  window.PTPMHDV_AUTH=Object.freeze({token:()=>accessToken,user:()=>user});
  button.addEventListener('click',()=>{if(accessToken){clear();}else{dialog.showModal();}});
  dialog.querySelector('#auth-cancel').addEventListener('click',()=>dialog.close());
  dialog.querySelector('form').addEventListener('submit',async event=>{
    event.preventDefault();const submit=dialog.querySelector('button[type=submit]');submit.disabled=true;
    const error=dialog.querySelector('#auth-error');error.textContent='';
    try {
      const response=await fetch(window.DASHBOARD_CONFIG.authLoginUrl,{method:'POST',credentials:'omit',
        headers:{'Content-Type':'application/json'},body:JSON.stringify({
          username:dialog.querySelector('#auth-username').value,
          password:dialog.querySelector('#auth-password').value})});
      const result=await response.json();
      if(!response.ok)throw new Error(result.message||'Đăng nhập không thành công.');
      accessToken=result.accessToken;user=result.user;
      dialog.querySelector('#auth-password').value='';
      button.textContent=user.username+' · Đăng xuất';dialog.close();
      clearTimeout(expiresTimer);expiresTimer=setTimeout(clear,result.expiresIn*1000);
      window.dispatchEvent(new Event('auth:changed'));
    } catch(err){error.textContent=err instanceof TypeError?'Chưa kết nối được Auth service cổng 8082.':err.message;}
    finally{submit.disabled=false;}
  });
  if(location.protocol!=='file:'&&new URLSearchParams(location.search).get('mode')!=='demo')dialog.showModal();
})();
