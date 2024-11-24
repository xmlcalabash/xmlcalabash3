(function() {
  let tableid = 0;

  function toggle(table, type, show) {
    table.querySelectorAll(`tbody tr.${type}`).forEach(row => {
      row.style.display = show ? "table-row" : "none";
    });
  } 

  function add_checkbox(table, div, type, label) {
    if (div.children.length != 0) {
      div.appendChild(document.createTextNode("; "))
    }

    const check = document.createElement("input");
    check.setAttribute("type", "checkbox");
    check.setAttribute("id", `chk.${type}_${tableid}`);
    check.setAttribute("checked", "checked");
    div.appendChild(check);
    div.appendChild(document.createTextNode(label));
    check.addEventListener('change', (event) => {
      toggle(table, type, event.target.checked);
    });
  }

  function filter(table) {
    tableid++

    let pass = table.querySelectorAll("tr.pass")
    let skip = table.querySelectorAll("tr.skip")
    let fail = table.querySelectorAll("tr.fail")
    if (skip.length > 0 || fail.length > 0) {
      const div = document.createElement("div");
      div.setAttribute("class", "toggles");
      add_checkbox(table, div, "pass", " show passed");
      if (skip.length > 0) {
        add_checkbox(table, div, "skip", " show skipped");
      }
      if (fail.length > 0) {
        add_checkbox(table, div, "fail", " show failed");
      }

/*
      let span1 = `<input type='checkbox' id='chk.pass_${tableid}' checked='checked'/> show passed`;
      let span2 = '';
      let span3 = '';

      if (skip.length > 0) {
        span2 = `; <input type='checkbox' id='chk.skip_${tableid}' checked='checked'/> show skipped`;
      }

      if (fail.length > 0) {
        span3 = `; <input type='checkbox' id='chk.fail_${tableid}' checked='checked'/> show failed`;
      }

      div.innerHTML = span1 + span2 + span3 + "."
*/

      table.parentNode.insertBefore(div, table)

      

    }
  }

/*
    <div>
      <input type="checkbox" id="chk.pass" checked="checked"/> show passed;
      <input type="checkbox" id="chk.skip" checked="checked"/> show skipped;
      <input type="checkbox" id="chk.fail" checked="checked"/> show failed
    </div>
*/

  document.querySelectorAll("table").forEach(table => {
    filter(table)
  });
})();

