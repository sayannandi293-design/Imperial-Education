(async()=>{
 const form=document.querySelector('#login-form');
 const password=document.querySelector('#login-password');
 const toggle=document.querySelector('#toggle-password');
 const status=document.querySelector('#login-message');
 toggle.addEventListener('click',()=>{
  const show=password.type==='password';password.type=show?'text':'password';
  toggle.textContent=show?'Hide':'Show';toggle.setAttribute('aria-label',show?'Hide password':'Show password');
  toggle.setAttribute('aria-pressed',String(show));
 });
 const params=new URLSearchParams(location.search);
 if(params.has('error'))status.textContent='Incorrect Gmail / Email or password. Try again.';
 if(params.has('logout'))status.textContent='You have signed out successfully.';
 try{
  const response=await fetch('/api/account/csrf',{credentials:'same-origin',cache:'no-store'});
  if(!response.ok)throw Error('Security token could not be loaded. Refresh the page.');
  const token=await response.json();
  const hidden=document.createElement('input');
  hidden.type='hidden';hidden.name=token.parameterName||'_csrf';hidden.value=token.token;
  form.append(hidden);
 }catch(e){
  status.textContent=e.message;document.querySelector('#login-submit').disabled=true;
 }
})();