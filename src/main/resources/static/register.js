const form=document.querySelector('#register-form');
const status=document.querySelector('#register-message');
const button=document.querySelector('#register-button');
let csrf;
async function loadToken(){
 const r=await fetch('/api/account/csrf',{credentials:'same-origin'});
 if(!r.ok)throw Error('Unable to start registration.');
 csrf=await r.json();
}
loadToken().catch(e=>status.textContent=e.message);
form.addEventListener('submit',async e=>{
 e.preventDefault();status.textContent='';
 if(!csrf){status.textContent='Please refresh the page and try again.';return;}
 button.disabled=true;
 try{
  const r=await fetch('/api/account/register',{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({
   fullName:document.querySelector('#full-name').value.trim(),email:document.querySelector('#email').value.trim(),password:document.querySelector('#password').value
  })});
  if(r.status===409){status.textContent='This email is already registered. Please sign in.';return;}
  if(!r.ok){status.textContent='Could not create account. Check your name, email and password.';return;}
  form.reset();status.replaceChildren();
  const p=document.createElement('p');p.textContent='Account created successfully! You can now sign in.';
  const a=document.createElement('a');a.href='/login';a.textContent='Go to Sign In';status.append(p,a);
 }catch{status.textContent='Network error. Please try again.'}
 finally{button.disabled=false}
});