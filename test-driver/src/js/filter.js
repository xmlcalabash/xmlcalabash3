(function() {
  let tableid = 0;

  function toggle(table, type, show) {
    table.querySelectorAll(`tbody tr.${type}`).forEach(row => {
      row.style.display = show ? "table-row" : "none";
    });
  } 

  function add_checkbox(table, div, type, label, startChecked) {
    if (div.children.length != 0) {
      div.appendChild(document.createTextNode("; "))
    }

    const check = document.createElement("input");
    check.setAttribute("type", "checkbox");
    check.setAttribute("id", `chk.${type}_${tableid}`);
    if (startChecked) {
      check.setAttribute("checked", "checked");
    }
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
      add_checkbox(table, div, "pass", " show passed", skip.length == 0 && fail.length == 0);
      if (skip.length > 0) {
        add_checkbox(table, div, "skip", " show skipped", fail.length == 0);
      }
      if (fail.length > 0) {
        add_checkbox(table, div, "fail", " show failed", true);
      }

      table.parentNode.insertBefore(div, table)
    }

    if (skip.length > 0 || fail.length > 0) {
      toggle(table, "pass", false)
    }
    if (skip.length > 0 && fail.length > 0) {
      toggle(table, "skip", false)
    }
  }

  document.querySelectorAll("table").forEach(table => {
    filter(table)
  });
})();

