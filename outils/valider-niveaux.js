// Vérifie la géométrie des niveaux de docs/fenua.html : rondes des gardes, points clés, eau, cordes, points de vue.
// Usage : node outils/valider-niveaux.js
const fs=require('fs'), path=require('path');
const h=fs.readFileSync(path.join(__dirname,'..','docs','fenua.html'),'utf8');
const code=h.slice(h.indexOf('function b(x,z,w,d,y0,y1,type){'),h.indexOf('/* ============================================================\n   3. Géométrie'));
const {LEVELS,HAZARDS}=new Function('const TAU=Math.PI*2;'+code+';return {LEVELS,HAZARDS};')();
const inB=(x,z,q,m)=>x>q.minX-m&&x<q.maxX+m&&z>q.minZ-m&&z<q.maxZ+m;
const inW=(lv,x,z,m)=>(lv.water||[]).some(w=>Math.abs(x-w.x)<w.w/2+m&&Math.abs(z-w.z)<w.d/2+m);
let total=0;
LEVELS.forEach((F,li)=>{const lv=F(), bad=[], B=lv.boxes;
  const solid=(x,y,z,m)=>B.find(q=>inB(x,z,q,m)&&y<q.y1-0.05&&y+1.7>q.y0);
  lv.guards.forEach((g,gi)=>{const y=g.y||0,P=g.path;
    for(let k=0;k<P.length;k++){const A=P[k],C=P[(k+1)%P.length];
      for(let t=0;t<=1;t+=0.02){const x=A[0]+(C[0]-A[0])*t,z=A[1]+(C[1]-A[1])*t;
        const q=solid(x,y,z,0.45); if(q){bad.push('ronde '+g.name+'#'+gi+' heurte '+q.type+' en '+x.toFixed(1)+','+z.toFixed(1));break;}
        if(y<0.5&&inW(lv,x,z,0.3)){bad.push('ronde '+g.name+' dans l\'eau');break;}
        if(y>0.5&&!B.some(q=>inB(x,z,q,0)&&Math.abs(q.y1-y)<0.3)){bad.push('ronde en hauteur '+g.name+' dans le vide');break;}}}});
  const pts=[['départ',lv.start],['sortie',lv.exit],...lv.lights.map(t=>['flambeau',t]),...lv.hides.map(h=>['cachette '+h.kind,h]),
    ...lv.pickups.map(p=>['objet',p]),...lv.inter.map(p=>['interaction',p]),
    ...(HAZARDS[li]||[]).flatMap(h=>[['impact',{x:h.x,z:h.z}],['poteau',{x:h.px,z:h.pz}]])];
  for(const [n,p] of pts){const q=solid(p.x,0,p.z,n.startsWith('cachette')?0:0.3); if(q) bad.push(n+' dans '+q.type+' en '+p.x+','+p.z);}
  (lv.ropes||[]).forEach((r,ri)=>[r.a,r.b].forEach(e=>{ if(!B.some(q=>inB(e[0],e[2],q,0.45)&&Math.abs(q.y1-e[1])<0.8)) bad.push('corde '+ri+' : extrémité sans appui'); }));
  if(lv.vista&&!B.some(q=>inB(lv.vista.x,lv.vista.z,q,0)&&Math.abs(q.y1-lv.vista.y)<0.1)) bad.push('point de vue sans sommet');
  lv.guards.filter(g=>g.isTarget&&g.y).forEach(g=>{ if(!B.some(q=>inB(g.path[0][0],g.path[0][1],q,0)&&Math.abs(q.y1-g.y)<0.1)) bad.push('cible '+g.name+' sans toit'); });
  total+=bad.length; console.log('Niveau '+(li+1)+' « '+lv.name+' » : '+(bad.length?bad.length+' problème(s)':'0 conflit'));
  bad.slice(0,12).forEach(x=>console.log('   · '+x));
});
console.log(total?'TOTAL '+total+' problème(s)':'Géométrie : 0 conflit sur les '+LEVELS.length+' niveaux');
process.exit(total?1:0);
