const params=new URLSearchParams(location.search);
const courseId=Number(params.get('course'));
const msg=document.querySelector('#checkout-message');
const payment=document.querySelector('#payment-information');
let csrf,course;
function text(selector,value){document.querySelector(selector).textContent=value}
async function init(){
 try{
  if(!Number.isSafeInteger(courseId)||courseId<=0)throw Error('Invalid course. Return to the course list.');
  const [cr,pr,tr]=await Promise.all([fetch('/api/courses/'+courseId),fetch('/api/payments/instructions'),fetch('/api/account/csrf')]);
  if(!cr.ok)throw Error('Course not available.');
  if(!pr.ok||!tr.ok)throw Error('Please sign in as a student to continue.');
  course=await cr.json();const instructions=await pr.json();csrf=await tr.json();
  text('#checkout-course',course.title);
  text('#checkout-price','₹'+(course.priceInPaise/100).toLocaleString('en-IN',{minimumFractionDigits:2}));
  if(!instructions.upiId){msg.textContent='UPI payments are not configured yet. Please check back later.';return}
  text('#payment-payee',instructions.payeeName);
  text('#upi-address',instructions.upiId);
  if(instructions.qrImageUrl){
   try{const url=new URL(instructions.qrImageUrl);if(url.protocol==='https:'){
     const img=document.querySelector('#upi-qr');img.src=url.href;img.hidden=false;
     img.onerror=()=>{img.hidden=true};
   }}catch{}
  }
  const uri='upi://pay?'+new URLSearchParams({pa:instructions.upiId,pn:instructions.payeeName,am:(course.priceInPaise/100).toFixed(2),cu:'INR',tn:'Imperial Education - '+course.title}).toString();
  const link=document.querySelector('#upi-launch');link.href=uri;link.hidden=false;
  payment.hidden=false;
 }catch(e){msg.textContent=e.message}
}
document.querySelector('#payment-form').addEventListener('submit',async e=>{
 e.preventDefault();msg.textContent='';
 if(!csrf||!course){msg.textContent='Please reload the page.';return}
 const btn=document.querySelector('#submit-utr');btn.disabled=true;
 try{
  const response=await fetch('/api/student/payments',{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/json',[csrf.headerName]:csrf.token},body:JSON.stringify({courseId:course.id,utr:document.querySelector('#payment-utr').value.trim()})});
  if(response.status===409){msg.textContent='A request is already pending, the reference was already used, or you are enrolled. Check My Account.';return}
  if(!response.ok){msg.textContent='Unable to submit. Check your UTR and try again.';return}
  payment.hidden=true;msg.replaceChildren();
  const p=document.createElement('p');p.textContent='Payment reference submitted. Status: PENDING. Your course will be available only after the administrator verifies receipt.';
  const a=document.createElement('a');a.href='/account.html';a.textContent='View payment status in My Account';
  msg.append(p,a);
 }catch{msg.textContent='Network error. Please try again.'}
 finally{btn.disabled=false}
});
init();