(async()=>{
 const token=await fetch('/api/account/csrf').then(r=>r.ok?r.json():null).catch(()=>null);
 if(!token)return;
 async function api(path,method='GET',body){
  const r=await fetch(path,{method,credentials:'same-origin',headers:{[token.headerName]:token.token,...(body?{'Content-Type':'application/json'}:{})},body:body?JSON.stringify(body):undefined});
  if(!r.ok)throw Error('Request failed (HTTP '+r.status+').');
  return r.status===204?null:r.json();
 }
 function item(area,title,info){
  const row=document.createElement('div');row.className='payment-row';
  const h=document.createElement('strong');h.textContent=title;
  const p=document.createElement('p');p.textContent=info;row.append(h,p);area.append(row);return row;
 }
 async function wishlist(){
  const area=document.querySelector('#student-wishlist');area.replaceChildren();
  try{
   const data=await api('/api/student/wishlist');
   if(!data.length)area.textContent='Your wishlist is empty. Save courses from the homepage.';
   for(const w of data){
    const row=item(area,w.title,'₹'+(w.priceInPaise/100).toLocaleString('en-IN'));
    const link=document.createElement('a');link.href='/checkout.html?course='+encodeURIComponent(w.courseId);link.textContent='Buy course';row.append(link);
    const remove=document.createElement('button');remove.className='secondary-button';remove.textContent='Remove';remove.style.marginLeft='12px';
    remove.onclick=async()=>{try{await api('/api/student/wishlist/'+w.courseId,'DELETE');await wishlist()}catch(e){alert(e.message)}};
    row.append(remove);
   }
  }catch(e){area.textContent=e.message}
 }
 async function tickets(){
  const area=document.querySelector('#student-tickets');area.replaceChildren();
  try{
   const data=await api('/api/student/tickets');
   if(!data.length)area.textContent='No support tickets yet.';
   for(const t of data)item(area,'#'+t.id+' · '+t.subject,t.status+' · '+t.message+(t.adminReply?' · Support reply: '+t.adminReply:''));
  }catch(e){area.textContent=e.message}
 }
 document.querySelector('#student-ticket-form').onsubmit=async e=>{
  e.preventDefault();const status=document.querySelector('#student-ticket-message');
  try{
   await api('/api/student/tickets','POST',{subject:document.querySelector('#ticket-subject').value.trim(),message:document.querySelector('#ticket-message').value.trim()});
   status.textContent='Your ticket was sent.';e.target.reset();await tickets();
  }catch(err){status.textContent=err.message}
 };
 async function reviewCourses(){
  const select=document.querySelector('#student-review-course');select.replaceChildren();
  try{
   const courses=await api('/api/student/my-courses');
   if(!courses.length){const o=document.createElement('option');o.value='';o.textContent='No approved courses';select.append(o);return}
   for(const c of courses){const option=document.createElement('option');option.value=c.courseId;option.textContent=c.title;select.append(option)}
  }catch(e){document.querySelector('#student-review-message').textContent=e.message}
 }
 document.querySelector('#student-review-form').onsubmit=async e=>{
  e.preventDefault();const status=document.querySelector('#student-review-message');
  const id=document.querySelector('#student-review-course').value;
  if(!id){status.textContent='An approved course is required.';return}
  try{
   const r=await api('/api/student/courses/'+encodeURIComponent(id)+'/review','POST',{rating:Number(document.querySelector('#student-review-rating').value),comment:document.querySelector('#student-review-comment').value.trim()});
   status.textContent=r.message;e.target.reset();
  }catch(err){status.textContent=err.message}
 };
 async function receipts(){
  const area=document.querySelector('#my-payments');
  if(!area)return;
  try{
   const payments=await api('/api/student/payments');
   const rows=area.querySelectorAll('.payment-row');
   payments.forEach((p,i)=>{
    if(!rows[i])return;
    const button=document.createElement('button');button.type='button';button.className='secondary-button';
    button.textContent='View receipt · IE-'+p.id;
    button.onclick=async()=>{
     try{
      const receipt=await api('/api/student/payments/'+p.id+'/receipt');
      const content=[
       'IMPERIAL EDUCATION - PAYMENT RECORD',
       'Order: '+receipt.orderId,
       'Student: '+receipt.student,
       'Course: '+receipt.course,
       'Amount: INR '+(receipt.amountInPaise/100).toFixed(2),
       'Status: '+receipt.status,
       'UTR: '+receipt.utr,
       'Submitted: '+receipt.submittedAt,
       'Reviewed: '+(receipt.reviewedAt||'Awaiting verification'),
       'Note: This is a payment record, not a tax invoice.'
      ].join('\\n');
      const blob=new Blob([content],{type:'text/plain;charset=utf-8'});
      const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download='imperial-'+receipt.orderId+'.txt';a.click();
      setTimeout(()=>URL.revokeObjectURL(url),1000);
     }catch(e){alert(e.message)}
    };
    rows[i].append(button);
   });
  }catch{}
 }
 await Promise.all([wishlist(),tickets(),reviewCourses()]);
 // Original account.js may still be rendering; retrieve receipt rows after its async requests.
 const ready=setInterval(()=>{
  const area=document.querySelector('#my-payments');
  if(area&&area.querySelector('.payment-row')){clearInterval(ready);receipts()}
 },300);
 setTimeout(()=>clearInterval(ready),9000);
})();