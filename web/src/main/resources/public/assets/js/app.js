
document.addEventListener("DOMContentLoaded",()=>{
  const page=document.body.dataset.page;
  const toast=(msg)=>{const el=document.createElement("div");el.className="toast";el.textContent=msg;document.body.appendChild(el);setTimeout(()=>el.remove(),3200)};
  document.querySelectorAll("[data-toast]").forEach(el=>el.addEventListener("click",e=>{e.preventDefault();toast(el.dataset.toast)}));
  document.querySelectorAll("[data-open-modal]").forEach(el=>el.addEventListener("click",()=>document.getElementById(el.dataset.openModal)?.classList.add("open")));
  document.querySelectorAll("[data-close-modal]").forEach(el=>el.addEventListener("click",()=>el.closest(".modal")?.classList.remove("open")));
  document.querySelectorAll(".modal").forEach(el=>el.addEventListener("click",e=>{if(e.target===el)el.classList.remove("open")}));
  document.querySelectorAll(".demo-form").forEach(form=>form.addEventListener("submit",e=>{e.preventDefault();toast("Formulario de demostración. Conectá esta acción con el backend Javalin.");form.reset();form.closest(".modal")?.classList.remove("open")}));
  document.querySelector(".mobile-menu")?.addEventListener("click",()=>document.body.classList.toggle("menu-open"));
  if(page==="public-donations"||page==="tracking-donor"||page==="tracking-beneficiary"){
    const id=page==="public-donations"?"donations-map":"tracking-map";
    const map=L.map(id).setView([-34.70,-58.39],11);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png",{attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'}).addTo(map);
    const points=page==="public-donations"?[
      [-34.706,-58.392,"<b>Alimentos no perecederos</b><br>Comedor Los Pinos<br>Entrega confirmada"],
      [-34.662,-58.365,"<b>Ropa de abrigo</b><br>Fundación Puentes<br>Entrega confirmada"],
      [-34.760,-58.405,"<b>Útiles escolares</b><br>Centro La Esperanza<br>Entrega confirmada"]
    ]:[[-34.70,-58.39,"<b>Camión DT-04</b><br>En camino a Fundación Puentes<br>Última actualización: 10:15"]];
    points.forEach(p=>L.marker([p[0],p[1]]).addTo(map).bindPopup(p[2]));
    if(page!=="public-donations")L.polyline([[-34.735,-58.43],[-34.72,-58.415],[-34.70,-58.39],[-34.68,-58.37]],{color:"#27835a",weight:4,dashArray:"7 8"}).addTo(map);
    setTimeout(()=>map.invalidateSize(),250);
  }
  const csv=document.getElementById("process-csv");
  csv?.addEventListener("click",()=>{
    const file=document.getElementById("csv-file").files[0],out=document.getElementById("csv-feedback");
    if(!file){out.textContent="Seleccioná un archivo CSV antes de continuar.";return}
    if(!file.name.toLowerCase().endsWith(".csv")){out.textContent="El archivo debe tener extensión .csv.";return}
    out.textContent=`Archivo seleccionado: ${file.name} (${(file.size/1024).toFixed(1)} KB). La validación de filas y la importación real se realizarán en el backend.`;
  });
});
