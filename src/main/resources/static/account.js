async function init(){
 const welcome=document.querySelector('#welcome');
 try{
  const [p,t]=await Promise.all([fetch('/api/account/me'),fetch('/api/account/csrf')]);
  if(!p.ok||!t.ok)throw Error('Please sign in again.');
  const profile=await p.json(),csrf=await t.json();
  welcome.textContent='Welcome, '+profile.fullName+' ('+profile.email+')';
  const hidden=document.createElement('input');hidden.type='hidden';hidden.name='_csrf';hidden.value=csrf.token;
  document.querySelector('#logout-form').append(hidden);
  const [cr,pr]=await Promise.all([fetch('/api/student/my-courses'),fetch('/api/student/payments')]);
  if(!cr.ok||!pr.ok)throw Error('Unable to load your courses.');
  const courses=await cr.json(),payments=await pr.json();
  const mine=document.querySelector('#my-courses'),pending=document.querySelector('#my-payments');
  mine.replaceChildren();pending.replaceChildren();
  if(!courses.length)mine.textContent='No approved courses yet. Explore courses to get started.';
  for(const c of courses){
   const row=document.createElement('div');row.className='payment-row';
   const title=document.createElement('strong');title.textContent=c.title;
   const note=document.createElement('p');note.textContent='Enrollment approved.';
   const materialsBox=document.createElement('div');materialsBox.textContent='Loading lessons…';
   row.append(title,note,materialsBox);mine.append(row);
   try{
    const response=await fetch('/api/student/courses/'+encodeURIComponent(c.courseId)+'/materials');
    if(!response.ok)throw Error('Could not load course materials');
    const materials=await response.json();materialsBox.replaceChildren();
    if(!materials.length){materialsBox.textContent='Lessons and notes have not been added yet.';continue}
    for(const m of materials){
     const line=document.createElement('p');const link=document.createElement('a');
     link.href=m.url;link.target='_blank';link.rel='noopener noreferrer';
     link.textContent=(m.type==='PDF'?'📄 PDF: ':'🎬 Video: ')+m.title;
     line.append(link);materialsBox.append(line);
    }
   }catch(e){materialsBox.textContent=e.message}
  }
  if(!payments.length)pending.textContent='No payment requests yet.';
  for(const payment of payments){
   const row=document.createElement('div');row.className='payment-row';
   const title=document.createElement('strong');title.textContent=payment.courseTitle;
   const label=document.createElement('p');label.textContent='₹'+(payment.amountInPaise/100).toFixed(2)+' · Reference '+payment.utr+' · '+payment.status;
   row.append(title,label);pending.append(row);
  }
 }catch(e){welcome.textContent=e.message}
}
init();