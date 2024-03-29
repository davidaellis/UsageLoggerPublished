document.addEventListener('DOMContentLoaded', function() {
    initializeSortable();
    initializeButtonFunction();
});

function initializeSortable() {
    sortable = $("#sortable");
    sortable.sortable({
        cursor: "move"
    })
}

function sanityCheck() {
    var checked_option = $('input[name="option[]"]:checked').length;
    if (checked_option || document.getElementById("past_usage").checked) {
        return true;
    } else {
        $("#sanity_check").show();
        window.onclick = function(event) {
            if (event.target == document.getElementById("sanity_check")) {
                $("#sanity_check").hide()
            }
        }
        $("#close_warning").click(function() {
            $("#sanity_check").hide()
        })
        return false;
    }
}

function initializeButtonFunction() {
    document.getElementById("show_instructions").onclick = function() {
        $("#my_modal").show();
        window.onclick = function(event) {
            if (event.target == document.getElementById("my_modal")) {
                $("#my_modal").hide();
            }
        }
    }

    $("#finished").click(function() {
       var rt_sanity = sanityCheck();
       if (rt_sanity === true) {
            writeQR(getTextForQR());
            window.onclick = function(event) {
                if (event.target == document.getElementById("qr_modal")) {
                    $("#qr_modal").hide()
                }
            }
        }
    });

    $("#close_instructions").click(function() {
        $("#my_modal").hide()
    })
    $("#close_qr").click(function() {
        $("#qr_modal").hide()
    })
    $("#contextual").click(function() {
        updateSortableContent("contextual")
        $('#c_installed_apps').attr('checked', true);
        $('#c_permissions').attr('checked', true);
        $('#c_responses').attr('checked', true);
    })
    $("#continuous_log").click(function() {
        updateSortableContent("continuous")
        $('#screen_usage').attr('checked', true);
        $('#app_usage').attr('checked', true);
        $('#notification').attr('checked', true);
        $('#installed_apps').attr('checked', true);
    })
    $("#past_usage").click(function() {
        updateSortableContent("past_usage")
    })
    $("#save_qr").click(function() {
        saveQR()
    })
}

function saveQR() {
    var childrenOfQrTarget = $("#qr_target").children();
    childrenOfQrTarget[0].id = "actual_qr"
    console.log(childrenOfQrTarget[0].id)
    console.log(childrenOfQrTarget[0])
    Canvas2Image.saveAsPNG(childrenOfQrTarget[0], 400, 400, "qrcode.png")
}

function updateSortableContent(elementToUpdate) {
    var elementsInSortable = $("#sortable li");
    var elementPresent = false;

    elementsInSortable.each(function(index, li) {
        if (li.id == elementToUpdate + '_li') {
            elementPresent = true;
        }
    })

    if (elementPresent) {
        removeElementFromSortable(elementToUpdate);
    } else {
        addElementToSortable(elementToUpdate);
    }
}

function addElementToSortable(elementToAdd) {
    switch (elementToAdd) {
        case "continuous":
            $("#continuous_log_options").show();
            $("#sortable").append('<li id=' + elementToAdd + '_li class="draggable">Continuous Logging</li>')
            break;
        case "past_usage":
            $("#past_usage_options").show();
            $("#sortable").append('<li id=' + elementToAdd + '_li class="draggable">Past Usage</li>')
            break;
        case "contextual":
            $("#contextual_options").show();
            $("#sortable").append('<li id=' + elementToAdd + '_li class="draggable">Contextual Data</li>')
            break;
        default:
    }
}

function removeElementFromSortable(elementToRemove) {
    $("#" + elementToRemove + "_li").remove();
    switch (elementToRemove) {
        case "continuous":
            $("#continuous_log_options").hide();
            break;
        case "past_usage":
            $("#past_usage_options").hide();
            break;
        case "contextual":
            $("#contextual_options").hide();
            break;
        default:
    }
}

function getTextForQR() {
    var toReturn = "{";
    var order = establishOrder();
    toReturn += "\n";

   if (document.getElementById("contextual").checked) {
        toReturn += "{C:T,";

        if (document.getElementById("c_installed_apps").checked) {
            toReturn += "{I:T,"
        } else {
            toReturn += "{I:F,"
        }
        if (document.getElementById("c_permissions").checked) {
            toReturn += "P:T,"
        } else {
            toReturn += "P:F,"
        }
        if (document.getElementById("c_responses").checked) {
            toReturn += "R:T"
        } else {
            toReturn += "R:F"
        }

        toReturn += "}"
        toReturn += ",O:" +
            order.indexOf('c')
        toReturn += "}"
        toReturn += "\n";
    } else {
        toReturn += "{C:F}";
        toReturn += "\n";
    }

    if (document.getElementById("past_usage").checked) {
        toReturn += "{U:T,";
        var daysToMonitor = document.getElementById("days_to_monitor");
        var result_days_monitor = 0;
        switch (daysToMonitor.value) { // fixed tk, Aug. 2022
            case "one":
                result_days_monitor = 1;
                break;
            case "two":
                result_days_monitor = 2;
                break;
            case "three":
                result_days_monitor = 3;
                break;
            case "four":
                result_days_monitor = 4;
                break;
            case "five":
                result_days_monitor = 5;
                break;
            default:
        }

        toReturn += "{D:" + result_days_monitor + "},"
        toReturn += "O:" + order.indexOf('u');
        toReturn += "}"
        toReturn += "\n";
    } else {
        toReturn += "{U:F}";
        toReturn += "\n";
    }

    if (document.getElementById("continuous_log").checked) {
        toReturn += "{P:T,";
        if (document.getElementById("screen_usage").checked) {
            toReturn += "{S:T,"
        } else {
            toReturn += "{S:F,"
        }
        if (document.getElementById("app_usage").checked) {
            toReturn += "A:T,"
        } else {
            toReturn += "A:F,"
        }
        if (document.getElementById("notification").checked) {
            toReturn += "N:T,"
        } else {
            toReturn += "N:F,"
        }
        if (document.getElementById("installed_apps").checked) {
            toReturn += "I:T}"
        } else {
            toReturn += "I:F}"
        }
        toReturn += ",O:" + order.indexOf("p");
        toReturn += "}"
    } else {
        toReturn += "{P:F}";
    }

    toReturn += "\n";
    toReturn += "}";
    console.log(toReturn);
    return toReturn;
}

function establishOrder() {
    var order = ['n', 'n', 'n'];
    var listItems = $("#sortable li");

    listItems.each(function(idx, li) {
        console.log(li.id);
        if (li.id == "contextual_li") {
            order[idx] = 'c';
        }
        if (li.id == "past_usage_li") {
            order[idx] = 'u';
        }
        if (li.id == "continuous_li") {
            order[idx] = 'p';
        }
    });
    console.log(order);
    return order;
}

function writeQR(toRelay) {
    $('#qr_modal').show()
    $('#qr_target').empty();
    jQuery('#qr_target').qrcode({
        text: toRelay
    });
}