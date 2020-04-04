document.addEventListener('DOMContentLoaded',function(){
  initializeSortable();
  initializeButtonFunction();
});

function initializeSortable(){
  sortable = $("#sortable");
  sortable.sortable({
    cursor: "move"
  })
}

function initializeButtonFunction(){
  document.getElementById("showInstructions").onclick = function(){
    $("#myModal").show();
    var span = document.getElementsByClassName("close")[0];
    span.onclick = function(){
      $("#myModal").hide()
    }
    window.onclick = function(event) {
      if (event.target == document.getElementById("myModal")) {
          $("#myModal").hide()
      }
    }
  }
  $("#contextual").click(function(){updateSortableContent("contextual")})
  $("#prospectiveLogging").click(function(){updateSortableContent("propsective")})
  $("#pastUsage").click(function(){updateSortableContent("pastUsage")})
  $("#finished").click(function(){writeQR(getTextForQR());
    window.onclick = function(event) {
      if (event.target == document.getElementById("qrModal")) {
          $("#qrModal").hide()
      }
    }})
  $("#closeQR").click(function(){$("#qrModal").hide()})
  $("#saveQR").click(function(){saveQR()})
}

function saveQR(){
  var chilrenOfQrTarget = $("#QRtarget").children();
  chilrenOfQrTarget[0].id = "actualQR"
  console.log(chilrenOfQrTarget[0].id)
  console.log(chilrenOfQrTarget[0])
  Canvas2Image.saveAsPNG(chilrenOfQrTarget[0], 400,400)
}

function updateSortableContent(elementToUpdate){
  var elementsInSortable = $("#sortable li");
  var elementPresent = false;
  elementsInSortable.each(function(index, li){
    if(li.id == elementToUpdate+'li'){
      elementPresent = true;
    }
  })
  if(elementPresent){
    removeElementFromSortable(elementToUpdate);
  }else{
    addElementToSortable(elementToUpdate);
  }
}

function addElementToSortable(elementToAdd){

  switch (elementToAdd) {
    case "propsective":
      $("#prospectiveLoggingOptions").show();
        $("#sortable").append('<li id='+elementToAdd+'li class="draggable">Prospective logging</li>')
      break;
    case "pastUsage":
      $("#pastUsageOptions").show();
      $("#sortable").append('<li id='+elementToAdd+'li class="draggable">Past Usage</li>')
      break;
    case "contextual":
      $("#contextualOptions").show();
      $("#sortable").append('<li id='+elementToAdd+'li class="draggable">Contextual</li>')
    break;
    default:
  }
}

function removeElementFromSortable (elementToRemove){
  $("#" + elementToRemove + "li").remove();
  switch (elementToRemove) {
    case "propsective":
      $("#prospectiveLoggingOptions").hide();
      break;
    case "pastUsage":
      $("#pastUsageOptions").hide();
      break;
    case "contextual":
      $("#contextualOptions").hide();
      break;
    default:
  }
}

function getTextForQR(){
  var toReturn = "{";
  var order = establishOrder();
  toReturn += "\n";
  if(document.getElementById("contextual").checked){
    toReturn+= "{C:T,";
      if(document.getElementById("cInstalledApps").checked){
        toReturn += "{I:T,"
      }else{
        toReturn += "{I:F,"
      }
      if(document.getElementById("cPermsissions").checked){
        toReturn += "P:T,"
      }else{
        toReturn += "P:F,"
      }
      if(document.getElementById("cResponses").checked){
        toReturn += "R:T"
      }else{
        toReturn += "R:F"
      }
      toReturn += "}"
      toReturn += ",O:"+
      order.indexOf('c')
      toReturn += "}"
      toReturn += "\n";
  }else{
    toReturn+= "{C:F}";
    toReturn += "\n";
  }
  if(document.getElementById("pastUsage").checked){
    toReturn+= "{U:T,";
    var daysToMonitor = document.getElementById("daysToMonitor");
    var resultOfdaysToMonitor = 0;
    switch (daysToMonitor.options[daysToMonitor.selectedIndex].text) {
      case "One":
        resultOfdaysToMonitor = 1;
        break;
      case "Two":
        resultOfdaysToMonitor = 2;
        break;
      case "Three":
        resultOfdaysToMonitor = 3;
        break;
      case "Four":
        resultOfdaysToMonitor = 4;
        break;
      case "Five":
        resultOfdaysToMonitor = 5;
        break;
      default:

    }
    toReturn += "{D:"+ resultOfdaysToMonitor +"},"
    toReturn+= "O:" + order.indexOf('u');
    toReturn += "}"
    toReturn+="\n";
  }else{
    toReturn+= "{U:F}";
    toReturn += "\n";
  }
  if(document.getElementById("prospectiveLogging").checked){
    toReturn+= "{P:T,";
    if(document.getElementById("screenUsage").checked){
      toReturn += "{S:T,"
    }else{
      toReturn += "{S:F,"
    }
    if(document.getElementById("appUsage").checked){
      toReturn += "A:T,"
    }else{
      toReturn += "A:F,"
    }
    if(document.getElementById("notification").checked){
      toReturn += "N:T,"
    }else{
      toReturn += "N:F,"
    }
    if(document.getElementById("installedApps").checked){
      toReturn += "I:T}"
    }else{
      toReturn += "I:F}"
    }
    toReturn += ",O:" + order.indexOf("p");
    toReturn += "}"
  }else{
    toReturn+= "{P:F}";
  }

  toReturn += "\n";
  toReturn+="}";
  console.log(toReturn);
  return toReturn;
}


function establishOrder(){
  var order = ['n', 'n', 'n'];
  var listItems = $("#sortable li");
  listItems.each(function(idx, li) {
    console.log(li.id);
    if(li.id == "contextualli"){
      order[idx] = 'c';
    }
    if(li.id == "pastUsageli"){
      order[idx] = 'u';
    }
    if(li.id == "propsectiveli"){
      order[idx] = 'p';
    }
  });
  console.log(order);
  return order;
}

function writeQR(toRelay){
  $('#qrModal').show()
  $('#QRtarget').empty();
  jQuery('#QRtarget').qrcode({
    text	:toRelay
  });

}
