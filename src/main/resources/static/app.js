const list=document.querySelector('#course-list');
fetch('/api/courses').then(async r=>{if(!r.ok)throw Error('Unable to load courses');return r.json()}).then(courses=>{
 list.replaceChildren();
 if(!courses.length){list.textContent='New courses are coming soon. Check back shortly.';return;}
 for(const c of courses){
 const card=document.createElement('article');card.className='course-card';
 const title=document.createElement('h3');title.textContent=c.title;
 const description=document.createElement('p');description.textContent=c.description||'Explore this course.';
 const price=document.createElement('div');price.className='price';price.textContent='₹'+(c.priceInPaise/100).toLocaleString('en-IN');
 card.append(title,description,price);list.append(card);
 }
}).catch(()=>list.textContent='Courses are temporarily unavailable.');
