(function (window, document) {
  "use strict";

  let tabs = {};

  function initToggle() {
    let hidepassed = document.getElementById("hidepassed");
    let hidefailed = document.getElementById("hidefailed");

    if (hidepassed && hidefailed) {
      hidepassed.onclick = hidePassed;
      hidepassed.checked = false;
      hidefailed.onclick = hideFailed;
      hidefailed.checked = false;
    }

    return true;
  }

  function hidePassed() {
    hide("pass");
  }

  function hideFailed() {
    hide("fail");
  }

  function hide(klass) {
    let divs = document.getElementsByTagName("div");
    console.log(divs);
    for (var i = 0; i < divs.length; ++i) {
      if (divs[i].className.indexOf("testcase") >= 0
          && divs[i].className.indexOf(klass) >= 0) {
        if (divs[i].className.indexOf("hidden") >= 0) {
          removeClass(divs[i], "hidden");
        } else {
          addClass(divs[i], "hidden");
        }
      }
    }
  }

  function changeElementClass(element, classValue) {
    if (element.getAttribute("className")) {
      element.setAttribute("className", classValue);
    } else {
      element.setAttribute("class", classValue);
    }
  }

  function getClassAttribute(element) {
    if (element.getAttribute("className")) {
      return element.getAttribute("className");
    } else {
      return element.getAttribute("class");
    }
  }

  function addClass(element, classValue) {
    changeElementClass(element, getClassAttribute(element) + " " + classValue);
  }

  function removeClass(element, classValue) {
    changeElementClass(element, getClassAttribute(element).replace(classValue, ""));
  }

  // Entry point.

  window.onload = function() {
    initToggle();
  };
} (window, window.document));
