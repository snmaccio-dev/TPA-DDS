document.addEventListener("DOMContentLoaded", function () {
  var contenedor = document.getElementById("seguimiento-map");
  var datos = document.getElementById("puntos-mapa");

  if (!contenedor || typeof L === "undefined") {
    return;
  }

  var puntos = [];

  try {
    puntos = JSON.parse(datos ? datos.textContent : "[]") || [];
  } catch (error) {
    puntos = [];
  }

  var mapa = L.map("seguimiento-map").setView([-34.70, -58.39], 11);

  L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
    attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
  }).addTo(mapa);

  var marcadores = puntos.map(function (punto) {
    var cuerpo = document.createElement("div");
    var titulo = document.createElement("b");

    titulo.textContent = punto.titulo;
    cuerpo.appendChild(titulo);
    cuerpo.appendChild(document.createElement("br"));
    cuerpo.appendChild(document.createTextNode(punto.detalle));

    return L.marker([punto.latitud, punto.longitud]).addTo(mapa).bindPopup(cuerpo);
  });

  if (marcadores.length === 1) {
    mapa.setView(marcadores[0].getLatLng(), 14);
  } else if (marcadores.length > 1) {
    mapa.fitBounds(L.featureGroup(marcadores).getBounds().pad(0.2));
  }

  setTimeout(function () {
    mapa.invalidateSize();
  }, 250);
});
