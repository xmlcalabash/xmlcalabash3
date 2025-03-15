const scaleSpan = document.querySelector("body p span.__scale")
if (scaleSpan) {
  scaleSpan.innerHTML = "<span></span> <button class='__minus'>-</button> / <button class='__plus'>+</button>"
  const spans = document.querySelectorAll("body p span.__scale *")
  const percent = spans[0];
  const minus = spans[1];
  const plus = spans[2];

  const svg = document.querySelector("svg > g");

  let before = null;
  let after = null;
  let scale = 1.0;

  function resize(factor) {
    if (attr) {
      scale = Math.round(parseFloat(scale * factor * 1000.0)) / 1000.0;
      svg.setAttribute("transform", `${before}${scale} ${scale}${after}`);
      percent.innerHTML = `${Math.round(scale * 100.0)}%`;
    }
  }

  const attr = svg ? svg.getAttribute("transform") : null;
  if (!attr) {
      console.log("No transform attribute on svg/g");
  } else {
    let pos = attr.indexOf("scale(");
    if (pos < -1) {
      console.log("No scale() in transform attribute on svg/g");
      attr = null
    } else {
      before = attr.substring(0, pos+6);
      after = attr.substring(pos+6);
      pos = after.indexOf(")");
      after = after.substring(pos)
    }
    resize(1);
  }

  minus.addEventListener("click", event => {
    resize(0.8);
  });

  plus.addEventListener("click", event => {
    resize(1.25);
  });
}
