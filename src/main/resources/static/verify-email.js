(async()=>{
 const result=document.querySelector('#verification-result');
 const resend=document.querySelector('#verification-resend');
 const token=new URLSearchParams(location.search).get('token');
 async function csrf(){
  const r=await fetch('/api/account/csrf',{credentials:'same-origin',cache:'no-store'});
  if(!r.ok)throw Error('Could not load security token. Refresh the page.');
  return r.json();
 }
 async function post(url,data){
  const t=await csrf();
  const r=await fetch(url,{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/json',[t.headerName]:t.token},body:JSON.stringify(data)});
  const response=await r.json().catch(()=>({}));
  if(!r.ok)throw Error(response.detail||response.message||'Request failed. Please retry.');
  return response;
 }
 if(token){
  result.textContent='Checking verification link…';
  try{const r=await post('/api/account/verification/verify',{token});result.textContent=r.message;history.replaceState(null,'','/verify-email.html');}
  catch(e){result.textContent=e.message}
 }
 resend.addEventListener('submit',async e=>{
  e.preventDefault();
  const button=resend.querySelector('button');button.disabled=true;result.textContent='Sending…';
  try{const r=await post('/api/account/verification/resend',{email:document.querySelector('#resend-email').value.trim()});result.textContent=r.message;}
  catch(e){result.textContent=e.message}
  finally{button.disabled=false}
 });
})();