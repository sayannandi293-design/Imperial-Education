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
 const buy=document.createElement('button');buy.type='button';buy.className='admin-button';buy.textContent='Buy Now';buy.addEventListener('click',()=>{window.location.href='/checkout.html?course='+encodeURIComponent(course.id)});
 const note=document.createElement('p');note.className='checkout-note';note.textContent='Pay by UPI and submit the transaction reference. Your course is unlocked only after manual verification.';
 details.append(h,p,price,document.createElement('hr'),buy,note);
 dialog.showModal();
}
let availableCourses=[];
const searchInput=document.querySelector('#course-search');
const categorySelect=document.querySelector('#course-category');
function courseCategory(title){
 const name=(title||'').toLowerCase();
 const categories=[['UPSC / Civil Services',/upsc|ias|civil service|psc/],['SSC / Railways',/ssc|railway|rrb/],['Banking',/bank|ibps|sbi|rbi/],['School / College',/class |school|college|university|bsc|btech/],['Technology',/web|coding|programming|python|java|software/]];
 return (categories.find(c=>c[1].test(name))||['Other'])[0];
}
function renderCourses(){
 const search=searchInput.value.toLowerCase().trim(),category=categorySelect.value;
 const filtered=availableCourses.filter(c=>(category==='all'||courseCategory(c.title)===category)&&
 (c.title+' '+(c.description||'')).toLowerCase().includes(search));
 list.replaceChildren();
 document.querySelector('#course-count').textContent=filtered.length+' course'+(filtered.length===1?'':'s')+' found';
 if(!filtered.length){list.textContent='No matching courses found. Try a different search.';return}
 for(const c of filtered){
  const card=document.createElement('article');card.className='course-card';
  const img=imageFor(c);if(img)card.append(img);
  const title=document.createElement('h3');title.textContent=c.title;
  const price=document.createElement('div');price.className='price';
  price.textContent='₹'+(c.priceInPaise/100).toLocaleString('en-IN');
  const actions=document.createElement('div');actions.className='course-actions';
  const view=document.createElement('button');view.className='admin-button';view.textContent='View Details';
  view.addEventListener('click',()=>showDetails(c));
  const buy=document.createElement('button');buy.className='secondary-button';buy.textContent='Buy Now';
  buy.addEventListener('click',()=>{window.location.href='/checkout.html?course='+encodeURIComponent(c.id)});
  actions.append(view,buy);card.append(title,price,actions);list.append(card);
 }
}
searchInput.addEventListener('input',renderCourses);
categorySelect.addEventListener('change',renderCourses);
fetch('/api/courses').then(async r=>{if(!r.ok)throw Error('Unable to load courses');return r.json()}).then(courses=>{
 availableCourses=courses;
 if(!courses.length){list.textContent='New courses are coming soon. Check back shortly.';return}
 const categories=[...new Set(courses.map(c=>courseCategory(c.title)))].sort();
 for(const category of categories){const option=document.createElement('option');option.value=category;option.textContent=category;categorySelect.append(option)}
 renderCourses();
}).catch(()=>list.textContent='Courses are temporarily unavailable.');


async function updateNavigation(){
 try{
  const response=await fetch('/api/session',{credentials:'same-origin',cache:'no-store'});
  if(!response.ok)return;
  const session=await response.json();
  if(!session.signedIn)return;
  document.querySelectorAll('[data-guest-nav]').forEach(element=>element.hidden=true);
  if(session.role==='STUDENT')document.querySelector('[data-student-nav]').hidden=false;
  if(session.role==='ADMIN')document.querySelector('[data-admin-nav]').hidden=false;
  const logout=document.querySelector('#home-logout');
  const csrf=document.createElement('input');
  csrf.type='hidden';csrf.name='_csrf';csrf.value=session.csrfToken;
  logout.append(csrf);logout.hidden=false;
 }catch(e){console.warn('Could not update navigation',e)}
}
updateNavigation();
