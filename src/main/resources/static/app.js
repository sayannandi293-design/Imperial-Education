const list=document.querySelector('#course-list');
const dialog=document.querySelector('#course-dialog');
const details=document.querySelector('#course-details');
document.querySelector('#close-course').addEventListener('click',()=>dialog.close());
dialog.addEventListener('click',e=>{if(e.target===dialog)dialog.close()});
function safePhoto(url){
 if(!url)return null;
 try{const u=new URL(url);return u.protocol==='https:'?u.href:null}catch{return null}
}
function imageFor(course){
 const url=safePhoto(course.imageUrl);
 if(!url)return null;
 const img=document.createElement('img');
 img.src=url;img.alt=course.title+' course image';img.loading='lazy';img.referrerPolicy='no-referrer';img.className='course-image';
 img.addEventListener('error',()=>img.replaceWith(Object.assign(document.createElement('div'),{className:'image-placeholder',textContent:'Course image unavailable'})));
 return img;
}
function showDetails(course){
 details.replaceChildren();
 const img=imageFor(course);if(img)details.append(img);
 const h=document.createElement('h3');h.textContent=course.title;
 const p=document.createElement('p');p.textContent=course.description||'No description available yet.';
 const price=document.createElement('strong');price.className='price';price.textContent='₹'+(course.priceInPaise/100).toLocaleString('en-IN',{minimumFractionDigits:2});
 const buy=document.createElement('button');buy.type='button';buy.className='admin-button';buy.disabled=true;buy.textContent='Buy Now · Coming soon';
 const note=document.createElement('p');note.className='checkout-note';note.textContent='Online payments and course enrollment are not active yet.';
 details.append(h,p,price,document.createElement('hr'),buy,note);
 dialog.showModal();
}
fetch('/api/courses').then(async r=>{if(!r.ok)throw Error('Unable to load courses');return r.json()}).then(courses=>{
 list.replaceChildren();
 if(!courses.length){list.textContent='New courses are coming soon. Check back shortly.';return;}
 for(const c of courses){
 const card=document.createElement('article');card.className='course-card';
 const img=imageFor(c);if(img)card.append(img);
 const title=document.createElement('h3');title.textContent=c.title;
 const price=document.createElement('div');price.className='price';price.textContent='₹'+(c.priceInPaise/100).toLocaleString('en-IN');
 const actions=document.createElement('div');actions.className='course-actions';
 const view=document.createElement('button');view.className='admin-button';view.textContent='View Details';view.addEventListener('click',()=>showDetails(c));
 const buy=document.createElement('button');buy.className='secondary-button';buy.textContent='Buy Now · Coming soon';buy.disabled=true;
 actions.append(view,buy);card.append(title,price,actions);list.append(card);
 }
}).catch(()=>list.textContent='Courses are temporarily unavailable.');
