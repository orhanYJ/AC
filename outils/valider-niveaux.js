// Vérifie la géométrie des niveaux de docs/fenua.html : rondes des gardes, points clés, eau, relief,
// cordes, points de vue. Usage : node outils/valider-niveaux.js
//
// Les gardes au sol suivent le sol comme dans le jeu : le relief (hillAt), et les marches de moins
// de 45 cm qu'on monte sans rien presser. Les gardes postés (champ y) restent à leur hauteur.
const fs=require('fs'), path=require('path');
const h=fs.readFileSync(path.join(__dirname,'..','docs','fenua.html'),'utf8');
const code=h.slice(h.indexOf('function b(x,z,w,d,y0,y1,type){'),h.indexOf('/* ============================================================\n   3. Géométrie'));
const {LEVELS,HAZARDS,hillAt}=new Function('const TAU=Math.PI*2;'+code+';return {LEVELS,HAZARDS,hillAt};')();
const SLOPE_MAX=1.2;                                   // comme dans le jeu
const inB=(x,z,q,m)=>x>q.minX-m&&x<q.maxX+m&&z>q.minZ-m&&z<q.maxZ+m;
const inW=(lv,x,z,m)=>(lv.water||[]).some(w=>Math.abs(x-w.x)<w.w/2+m&&Math.abs(z-w.z)<w.d/2+m);
// ce qui peut pousser ou se poser sur une pente
const SUR_PENTE={herbe:1,bush:1};
let total=0;
LEVELS.forEach((F,li)=>{const lv=F(), bad=[], B=lv.boxes, HL=lv.hills||[];
  const relief=(x,z)=>hillAt(x,z,HL);
  // le sol sous (x,z) pour qui arrive de la hauteur prev : relief, ou dessus d'une boîte qu'on peut monter
  const sol=(x,z,prev)=>{ let g=relief(x,z);
    for(const q of B) if(inB(x,z,q,0)&&q.y1<=prev+0.45&&q.y1>g) g=q.y1; return g; };
  // une boîte qui barre le passage à la hauteur y (les marches franchissables ne comptent pas)
  const solid=(x,y,z,m,marche)=>B.find(q=>inB(x,z,q,m)&&y<q.y1-0.05&&y+1.7>q.y0&&!(marche&&q.y1<=y+0.45));
  lv.guards.forEach((g,gi)=>{const poste=!!g.y, P=g.path;
    let y=poste?g.y:sol(P[0][0],P[0][1],0.2), stop=false;
    for(let k=0;k<P.length&&!stop;k++){const A=P[k],C=P[(k+1)%P.length];
      let px=A[0],pz=A[1],py=y;
      for(let t=0;t<=1;t+=0.02){const x=A[0]+(C[0]-A[0])*t,z=A[1]+(C[1]-A[1])*t;
        if(!poste){ y=sol(x,z,y); }
        const q=solid(x,y,z,0.45,!poste);
        if(q){bad.push('ronde '+g.name+'#'+gi+' heurte '+q.type+' en '+x.toFixed(1)+','+z.toFixed(1));stop=true;break;}
        if(!poste&&y<0.5&&inW(lv,x,z,0.3)){bad.push('ronde '+g.name+' dans l\'eau');stop=true;break;}
        if(poste&&!B.some(q=>inB(x,z,q,0)&&Math.abs(q.y1-y)<0.3)){bad.push('ronde en hauteur '+g.name+' dans le vide');stop=true;break;}
        if(!poste){ const dd=Math.hypot(x-px,z-pz), dh=relief(x,z)-relief(px,pz);
          if(dd>1e-3&&Math.abs(dh)/dd>SLOPE_MAX){bad.push('ronde '+g.name+' sur une pente trop raide en '+x.toFixed(1)+','+z.toFixed(1));stop=true;break;} }
        px=x;pz=z;py=y;}}});
  const pts=[['départ',lv.start],['sortie',lv.exit],...lv.lights.map(t=>['flambeau',t]),...lv.hides.map(h=>['cachette '+h.kind,h]),
    ...lv.pickups.map(p=>['objet',p]),...lv.inter.map(p=>['interaction',p]),
    ...(HAZARDS[li]||[]).flatMap(h=>[['impact',{x:h.x,z:h.z}],['poteau',{x:h.px,z:h.pz}]]),
    ...B.filter(q=>q.type==="palm"||q.type==="ora"||q.type==="uru").map(q=>['arbre '+q.type,q])];
  for(const [n,p] of pts){
    const arbre=n.startsWith('arbre');
    if(!arbre){ const q=solid(p.x,relief(p.x,p.z),p.z,n.startsWith('cachette')?0:0.3); if(q) bad.push(n+' dans '+q.type+' en '+p.x+','+p.z); }
    const kind=n.startsWith('cachette')?p.kind:null;
    if(!SUR_PENTE[kind]&&relief(p.x,p.z)>0.15) bad.push(n+' posé sur une colline en '+p.x+','+p.z+' (il y serait enterré)');
  }
  // un bâtiment n'est pas bâti sur une pente : il y serait enterré d'un côté
  B.forEach(q=>{ if(!["stone","wood","fare","tower","marche","crate","hull","tent","palis","marae"].includes(q.type)) return;
    let pire=0; for(let i=0;i<=4;i++) for(let j=0;j<=4;j++) pire=Math.max(pire,relief(q.minX+q.w*i/4,q.minZ+q.d*j/4));
    if(pire>0.15) bad.push(q.type+' en '+q.x+','+q.z+' bâti sur une colline ('+pire.toFixed(2)+' m)'); });
  // une colline ne monte pas dans l'eau
  (lv.water||[]).forEach((w,wi)=>{ let pire=0;
    for(let i=0;i<=8;i++) for(let j=0;j<=8;j++) pire=Math.max(pire,relief(w.x-w.w/2+w.w*i/8,w.z-w.d/2+w.d*j/8));
    if(pire>0.05) bad.push('eau '+wi+' recouverte par une colline ('+pire.toFixed(2)+' m)'); });
  const appui=(x,y,z,m)=>B.some(q=>inB(x,z,q,m)&&Math.abs(q.y1-y)<0.8)||Math.abs(relief(x,z)-y)<0.8;
  (lv.ropes||[]).forEach((r,ri)=>[r.a,r.b].forEach(e=>{ if(!appui(e[0],e[1],e[2],0.45)) bad.push('corde '+ri+' : extrémité sans appui'); }));
  if(lv.vista&&!B.some(q=>inB(lv.vista.x,lv.vista.z,q,0)&&Math.abs(q.y1-lv.vista.y)<0.1)
     &&Math.abs(relief(lv.vista.x,lv.vista.z)-lv.vista.y)>0.3) bad.push('point de vue sans sommet');
  lv.guards.filter(g=>g.isTarget&&g.y).forEach(g=>{ if(!B.some(q=>inB(g.path[0][0],g.path[0][1],q,0)&&Math.abs(q.y1-g.y)<0.1)) bad.push('cible '+g.name+' sans toit'); });
  total+=bad.length; console.log('Niveau '+(li+1)+' « '+lv.name+' » : '+(bad.length?bad.length+' problème(s)':'0 conflit'));
  bad.slice(0,14).forEach(x=>console.log('   · '+x));
});
console.log(total?'TOTAL '+total+' problème(s)':'Géométrie : 0 conflit sur les '+LEVELS.length+' niveaux');
process.exit(total?1:0);
