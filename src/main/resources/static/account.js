async function init(){
 const welcome=document.querySelector('#welcome');
 try{
  const [profileResponse,csrfResponse]=await Promise.all([fetch('/api/account/me'),fetch('/api/account/csrf')]);
  if(!profileResponse.ok||!csrfResponse.ok)throw Error('Please sign in again.');
  const profile=await profileResponse.json(),csrf=await csrfResponse.json();
  welcome.textContent='Welcome, '+profile.fullName+' ('+profile.email+')';
  const hidden=document.createElement('input');hidden.type='hidden';hidden.name='_csrf';hidden.value=csrf.token;
  document.querySelector('#logout-form').append(hidden);
 }catch(e){welcome.textContent=e.message}
}
init();