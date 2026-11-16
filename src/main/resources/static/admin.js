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
 if(!csrf)throw Error('Security token unavailable.');
 const response=await fetch(url,{method,headers:{[csrf.headerName]:csrf.token,...(data?{'Content-Type':'application/json'}:{})},body:data?JSON.stringify(data):undefined});
 if(!response.ok)throw Error('Request failed (HTTP '+response.status+'). Check the details and try again.');
 return response.status===204?null:response.json();
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
