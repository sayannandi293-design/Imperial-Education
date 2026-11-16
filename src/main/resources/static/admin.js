const form=document.querySelector('#course-form');
const list=document.querySelector('#admin-courses');
const message=document.querySelector('#form-message');
const titleInput=document.querySelector('#course-title');
const descriptionInput=document.querySelector('#course-description');
const priceInput=document.querySelector('#course-price');
const imageInput=document.querySelector('#course-image');
let editingId=null, courses=[],csrf=null;
async function init(){
 try{
  const tokenResponse=await fetch('/api/admin/csrf');
  if(!tokenResponse.ok)throw Error('Please log in again.');
  csrf=await tokenResponse.json();
  const logout=document.querySelector('#logout-form');
  const hidden=document.createElement('input');hidden.type='hidden';hidden.name='_csrf';hidden.value=csrf.token;logout.append(hidden);
  await reload();
  populateTelegramCourses();
  await loadTelegram();
  await loadPayments();
 }catch(e){message.textContent=e.message}
}
async function reload(){
 const response=await fetch('/api/admin/courses');
 if(!response.ok)throw Error('Could not load courses');
 courses=await response.json();
 render();
}
function render(){
 list.replaceChildren();
 if(!courses.length){list.textContent='No courses yet. Add your first course above.';return}
 for(const course of courses){
  const article=document.createElement('article');article.className='admin-course';
  const content=document.createElement('div');const heading=document.createElement('h3');
  heading.textContent=course.title;
  const detail=document.createElement('p');detail.textContent='₹'+(course.priceInPaise/100).toLocaleString('en-IN',{minimumFractionDigits:2})+' · '+(course.description||'No description');
  content.append(heading,detail);
  const buttons=document.createElement('div');buttons.className='actions';
  const edit=document.createElement('button');edit.className='secondary-button';edit.textContent='Edit';
  edit.onclick=()=>{editingId=course.id;titleInput.value=course.title;descriptionInput.value=course.description||'';priceInput.value=(course.priceInPaise/100).toFixed(2);imageInput.value=course.imageUrl||'';document.querySelector('#form-heading').textContent='Edit course';document.querySelector('#save-btn').textContent='Update Course';document.querySelector('#cancel-btn').hidden=false;form.scrollIntoView({behavior:'smooth'});};
  const remove=document.createElement('button');remove.className='danger-button';remove.textContent='Delete';
  remove.onclick=async()=>{if(!confirm('Delete this course?'))return;try{await request('/api/admin/courses/'+course.id,'DELETE');message.textContent='Course deleted.';await reload()}catch(e){message.textContent=e.message}};
  buttons.append(edit,remove);article.append(content,buttons);list.append(article);
 }
}
async function request(url,method,data){
 if(!csrf)throw Error('Security token unavailable. Please refresh and sign in as admin.');
 const response=await fetch(url,{method,credentials:'same-origin',redirect:'manual',headers:{'Accept':'application/json',[csrf.headerName]:csrf.token,...(data?{'Content-Type':'application/json'}:{})},body:data?JSON.stringify(data):undefined});
 if(response.type==='opaqueredirect'||response.status===401||response.status===403){
  throw Error('Admin session expired or access denied. Open /login, sign in again, then check Available courses before retrying.');
 }
 if(!response.ok){
  const contentType=response.headers.get('content-type')||'';
  let detail='';
  if(contentType.includes('application/json')){
   try{const body=await response.json();detail=body.detail||body.message||body.error||''}catch{}
  }
  throw Error('Save failed (HTTP '+response.status+'). '+detail+' Check Render Logs if the problem continues.');
 }
 if(response.status===204)return null;
 const contentType=response.headers.get('content-type')||'';
 if(!contentType.includes('application/json')){
  throw Error('Server returned an unexpected page instead of JSON. Check Available courses before submitting again; if not saved, sign in again and check Render Logs.');
 }
 return response.json();
}
function resetForm(){editingId=null;form.reset();document.querySelector('#form-heading').textContent='Add a new course';document.querySelector('#save-btn').textContent='Save Course';document.querySelector('#cancel-btn').hidden=true}
document.querySelector('#cancel-btn').onclick=resetForm;
form.onsubmit=async e=>{
 e.preventDefault();message.textContent='';
 const rupees=Number(priceInput.value);
 if(!Number.isFinite(rupees)||rupees<0||Math.round(rupees*100)>1000000000){message.textContent='Enter a valid price.';return}
 const payload={title:titleInput.value.trim(),description:descriptionInput.value.trim(),priceInPaise:Math.round(rupees*100),imageUrl:imageInput.value.trim()||null};
 if(!payload.title){message.textContent='Course title is required.';return}
 try{await request(editingId===null?'/api/admin/courses':'/api/admin/courses/'+editingId,editingId===null?'POST':'PUT',payload);resetForm();message.textContent='Course saved successfully.';await reload()}catch(err){message.textContent=err.message}
};
init();
async function loadPayments(){
 const container=document.querySelector('#payment-requests');
 const response=await fetch('/api/admin/payments');
 if(!response.ok){container.textContent='Unable to load payment requests.';return}
 const payments=await response.json();container.replaceChildren();
 if(!payments.length){container.textContent='No UPI payment requests yet.';return}
 for(const p of payments){
  const row=document.createElement('div');row.className='payment-row';
  const name=document.createElement('strong');name.textContent=p.courseTitle+' · ₹'+(p.amountInPaise/100).toFixed(2);
  const who=document.createElement('p');who.textContent=p.studentName+' ('+p.studentEmail+')';
  const ref=document.createElement('p');ref.textContent='UTR: '+p.utr+' · Status: '+p.status+' · Submitted: '+new Date(p.createdAt).toLocaleString();
  row.append(name,who,ref);
  if(p.status==='PENDING'){
   const actions=document.createElement('div');actions.className='actions';
   for(const action of ['approve','reject']){
    const btn=document.createElement('button');btn.className=action==='approve'?'admin-button':'danger-button';
    btn.type='button';btn.textContent=action==='approve'?'Approve verified payment':'Reject request';
    btn.onclick=async()=>{
     if(!confirm(action==='approve'?'Have you independently verified this UTR, recipient account, exact amount and payment in your bank statement?':'Reject this payment request?'))return;
     try{
      await request('/api/admin/payments/'+p.id+'/'+action.toLowerCase(),'POST');
      document.querySelector('#payment-message').textContent='Payment request '+action+'d.';
      await loadPayments();
     }catch(err){document.querySelector('#payment-message').textContent=err.message}
    };actions.append(btn);
   }row.append(actions);
  }
  container.append(row);
 }
}

function populateTelegramCourses(){
 const select=document.querySelector('#telegram-course');
 const selected=select.value;select.replaceChildren();
 for(const course of courses){
  const option=document.createElement('option');option.value=course.id;option.textContent=course.title;select.append(option);
 }
 if(courses.some(c=>String(c.id)===selected))select.value=selected;
}
async function loadTelegram(){
 const select=document.querySelector('#telegram-course'),input=document.querySelector('#telegram-url');
 const status=document.querySelector('#telegram-message');input.value='';status.textContent='';
 if(!select.value){status.textContent='Create a course first.';return}
 try{
  const response=await fetch('/api/admin/courses/'+encodeURIComponent(select.value)+'/telegram');
  if(!response.ok)throw Error('Unable to load group settings.');
  const result=await response.json();
  input.value=result.inviteUrl||'';
  status.textContent=result.inviteUrl?'Telegram group link is configured.':'No Telegram group set for this course.';
 }catch(e){status.textContent=e.message}
}
document.querySelector('#telegram-course').addEventListener('change',loadTelegram);
document.querySelector('#telegram-form').addEventListener('submit',async e=>{
 e.preventDefault();
 const status=document.querySelector('#telegram-message'),id=document.querySelector('#telegram-course').value;
 if(!id)return;
 try{
  await request('/api/admin/courses/'+encodeURIComponent(id)+'/telegram','PUT',{inviteUrl:document.querySelector('#telegram-url').value.trim()});
  status.textContent='Telegram group link saved for this course.';
 }catch(e){status.textContent=e.message}
});
document.querySelector('#remove-telegram').addEventListener('click',async()=>{
 const id=document.querySelector('#telegram-course').value;
 if(!id||!confirm('Remove Telegram group link for this course?'))return;
 try{
  await request('/api/admin/courses/'+encodeURIComponent(id)+'/telegram','DELETE');
  document.querySelector('#telegram-url').value='';
  document.querySelector('#telegram-message').textContent='Telegram group link removed.';
 }catch(e){document.querySelector('#telegram-message').textContent=e.message}
});
