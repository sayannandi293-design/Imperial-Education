const form=document.querySelector('#register-form');
const status=document.querySelector('#register-message');
const button=document.querySelector('#register-button');

async function loadToken(){
 const r=await fetch('/api/account/csrf',{credentials:'same-origin',cache:'no-store'});
 if(!r.ok)throw Error('Unable to initialize registration security (HTTP '+r.status+').');
 return await r.json();
}

form.addEventListener('submit',async e=>{
 e.preventDefault();
 status.textContent='';
 button.disabled=true;
 try{
  const csrf=await loadToken();
  const r=await fetch('/api/account/register',{
   method:'POST',credentials:'same-origin',cache:'no-store',
   headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},
   body:JSON.stringify({
    fullName:document.querySelector('#full-name').value.trim(),
    email:document.querySelector('#email').value.trim(),
    password:document.querySelector('#password').value
   })
  });
  if(r.ok){
   form.reset();status.replaceChildren();
   const p=document.createElement('p');p.textContent='Account created successfully! You can now sign in with your email and password.';
   const a=document.createElement('a');a.href='/login';a.textContent='Go to Sign In';
   status.append(p,a);return;
  }
  // Do not display raw server exception details or disclose sensitive information.
  const messages={
   400:'Please check your name, email format and password. The password must be 12–72 characters.',
   401:'Your session expired. Refresh the page and try again.',
   403:'Security check failed (HTTP 403). Refresh the page, then retry. If this continues, contact support with the HTTP code.',
   409:'This email address is already registered. Please use Sign In or Forgot Password.',
   429:'Too many attempts. Try again later.',
   503:'Registration is temporarily unavailable (HTTP 503). Please try again later.',
   500:'Server error (HTTP 500). The administrator should check Render logs at the time of this request.'
  };
  status.textContent=messages[r.status]||'Registration failed (HTTP '+r.status+'). Contact support with this status code.';
 }catch(err){
  status.textContent='Unable to complete registration: '+(err instanceof Error?err.message:'Network error');
 }finally{button.disabled=false}
});
