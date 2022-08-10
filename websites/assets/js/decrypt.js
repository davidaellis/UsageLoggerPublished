var __PDF_DOC,
	__CURRENT_PAGE,
	__TOTAL_PAGES,
	__PAGE_RENDERING_IN_PROGRESS = 0,
	__CANVAS = $('#pdf-canvas').get(0),
	__CANVAS_CTX = __CANVAS.getContext('2d'),
	__EXTRACTING_DATA = true,
	__TEXT_TO_EXPORT ="",
	__NUM_COLUMS = 0,
	__DATA = [],
	__ROW_NUM = 0,
	__COL_LEFT_VALS = [];

$("#password-container").hide();

function showPDF(pdf_url, pdf_password) {
	$("#pdf-loader").show();
	$("#password-container").hide();

	PDFJS.getDocument({ url: pdf_url, password: pdf_password }).then(function(pdf_doc) {
		__PDF_DOC = pdf_doc;
		__TOTAL_PAGES = __PDF_DOC.numPages;

		// Hide the pdf loader and show pdf container in HTML
		$("#pdf-loader").hide();
		$("#password-container").hide();
		$("#pdf-contents").show();
		$("#pdf-total-pages").text(__TOTAL_PAGES);

		// Show the first page
		showPage(1);
	}).catch(function(error) {
		$("#pdf-loader").hide();

		if(error.name == 'PasswordException') {
			$("#password-container").show();
			$("#pdf-password").val('');
			$("#password-message").text(error.code == 2 ? error.message : '');
		}
		else {
			$("#submit-pdf").show();
			alert(error.message);
		}
	});;
}

function showPage(page_no) {
	__PAGE_RENDERING_IN_PROGRESS = 1;
	__CURRENT_PAGE = page_no;

	// While page is being rendered hide the canvas and show a loading message
	$("#pdf-canvas").hide();
	$("#page-loader").show();
	$("#show-instructions").hide();

	// Update current page in HTML
	$("#pdf-current-page").text(page_no);

	// Fetch the page
	__PDF_DOC.getPage(page_no).then(function(page) {
		// As the canvas is of a fixed width we need to set the scale of the viewport accordingly
		var scale_required = __CANVAS.width / page.getViewport(1).width;

		// Get viewport of the page at required scale
		var viewport = page.getViewport(scale_required);

		// Set canvas height
		__CANVAS.height = viewport.height;

		var renderContext = {
			canvasContext: __CANVAS_CTX,
			viewport: viewport
		};

		if(__EXTRACTING_DATA){
			// Render the page contents in the canvas
			page.render(renderContext).then(function() {
				__PAGE_RENDERING_IN_PROGRESS = 0;

				// Show the canvas and hide the page loader
				$("#pdf-canvas").show();
				$("#page-loader").hide();

				// Return the text contents of the page after the pdf has been rendered in the canvas
				return page.getTextContent();
			}).then(function(textContent) {
				// Get canvas offset
				var canvas_offset = $("#pdf-canvas").offset();

				// Clear HTML for text layer
				$("#text-layer").html('');

				// Assign the CSS created to the text-layer element
				$("#text-layer").css({ left: canvas_offset.left + 'px', top: canvas_offset.top + 'px', height: __CANVAS.height + 'px', width: __CANVAS.width + 'px' });

				// Pass the data to the method for rendering of text over the pdf canvas.
				PDFJS.renderTextLayer({
				    textContent: textContent,
				    container: $("#text-layer").get(0),
				    viewport: viewport,
				    textDivs: []
				});
			}).then(function(){
				readPage();
				return
			}).then(function(){
					if(__CURRENT_PAGE != __TOTAL_PAGES ){
						showPage(__CURRENT_PAGE + 1);
				}
			else{
				commitToCsv();
				exportToCsv();
				}
			})
		}else{
			console.log("didn't extract");
			// Render the page contents in the canvas
			page.render(renderContext).then(function() {
				__PAGE_RENDERING_IN_PROGRESS = 0;

				// Show the canvas and hide the page loader
				$("#pdf-canvas").show();
				$("#page-loader").hide();

				// Return the text contents of the page after the pdf has been rendered in the canvas
				return page.getTextContent();
			}).then(function(textContent) {
				// Get canvas offset
				var canvas_offset = $("#pdf-canvas").offset();

				// Clear HTML for text layer
				$("#text-layer").html('');

				// Assign the CSS created to the text-layer element
				$("#text-layer").css({ left: canvas_offset.left + 'px', top: canvas_offset.top + 'px', height: __CANVAS.height + 'px', width: __CANVAS.width + 'px' });

				// Pass the data to the method for rendering of text over the pdf canvas.
				PDFJS.renderTextLayer({
						textContent: textContent,
						container: $("#text-layer").get(0),
						viewport: viewport,
						textDivs: []
				});
			});
		}
	})
}

// Upon click this should should trigger click on the #file-to-upload file input element
// This is better than showing the not-good-looking file input element
$("#upload-button").on('click', function() {
	$("#file-to-upload").trigger('click');
});

// When user chooses a PDF file
$("#file-to-upload").on('change', function() {
	// Validate whether PDF
    if(['application/pdf'].indexOf($("#file-to-upload").get(0).files[0].type) == -1) {
        alert('Error : The selected file is not a PDF');
        return;
    }

	$("#submit-pdf").hide();

	// Send the object url of the pdf
	showPDF(URL.createObjectURL($("#file-to-upload").get(0).files[0]), '' );
});

$("#submit-password").on('click', function() {
  __FILE_NAME = $("#file-to-upload").get(0).files[0].name;
	__FILE_NAME = __FILE_NAME.substring(0, (__FILE_NAME.length - 4) )
	showPDF(URL.createObjectURL($("#file-to-upload").get(0).files[0]), $("#pdf-password").val());
});

function getNumOfCol(){
	var cText = document.getElementById("text-layer").children;
	leftArr = []
	for (var i = 0; i < cText.length; i++){
		if(!leftArr.includes(cText[i].style.left)){
			leftArr.push(cText[i].style.left)
		}
	}
	__NUM_COLUMS = leftArr.length;
	console.log(__NUM_COLUMS)
	__COL_LEFT_VALS = leftArr;
	console.log(__COL_LEFT_VALS)
}

function returnUniqueRow(){
	var cText = document.getElementById("text-layer").children;
	topArr = []
	for (var i = 0; i < cText.length; i++){
		if(!topArr.includes(cText[i].style.top)){
			topArr.push(cText[i].style.top)
		}
	}
	return topArr
}

function returnTops(text){
	tops = []
	for (var i = 0; i < text.length; i++){
		tops.push(text[i].style.top)
	}
	return (tops);
}

function readPage(){
		if(document.getElementById("pdf-current-page").innerHTML == "1"){
			getNumOfCol()
		}
		standardRead(returnUniqueRow())
}

function standardRead(NumOfRows){
	var cText = document.getElementById("text-layer").children;
	var tops = returnTops(cText)

	currentRow = []
	atFinalColumn = true;
	for (var i = 0; i < cText.length; i++){
			if(cText[i].style.left == __COL_LEFT_VALS[0] && atFinalColumn){
				//started on new row
				currentRow.push({"row" : __ROW_NUM++})
				atFinalColumn = false;
			}
			if(cText[i].style.left == __COL_LEFT_VALS[0]){
				if(Object.keys(currentRow[currentRow.length-1]).includes("one")){
					currentRow[currentRow.length-1].one += cText[i].innerHTML
				}else{
					currentRow[currentRow.length-1].one = cText[i].innerHTML
				}
			}else if(cText[i].style.left == __COL_LEFT_VALS[1]){
				if(Object.keys(currentRow[currentRow.length-1]).includes("two")){
					currentRow[currentRow.length-1].two += cText[i].innerHTML
				}else{
					currentRow[currentRow.length-1].two = cText[i].innerHTML
				}
			}else if(cText[i].style.left == __COL_LEFT_VALS[2]){
				if(Object.keys(currentRow[currentRow.length-1]).includes("three")){
					currentRow[currentRow.length-1].three += cText[i].innerHTML
				}else{
					currentRow[currentRow.length-1].three = cText[i].innerHTML
				}
			}

			if(cText[i].style.left == __COL_LEFT_VALS[__NUM_COLUMS-1]){
				atFinalColumn = true;
			}

	}
	for (var i = 0; i < currentRow.length; i++){
		__DATA.push(currentRow[i])
	}
}

function commitToCsv(){
	for (var i = 0; i < __NUM_COLUMS; i++){
		__TEXT_TO_EXPORT+=("Col" +i +",")
	}
	if(__NUM_COLUMS > 1){
		__TEXT_TO_EXPORT = __TEXT_TO_EXPORT.slice(0, __TEXT_TO_EXPORT.length-1)
	}

	__TEXT_TO_EXPORT+="\n";
	for( var i = 0; i < __DATA.length; i++){
			daKeys = Object.keys(__DATA[i])
			if(daKeys.includes("one")){
				__TEXT_TO_EXPORT += __DATA[i].one;
			}
			if(daKeys.includes("two")){
				__TEXT_TO_EXPORT += ",";
				__TEXT_TO_EXPORT += __DATA[i].two;
			}
		 	if(daKeys.includes("three")){
				__TEXT_TO_EXPORT += ",";
				__TEXT_TO_EXPORT += __DATA[i].three;
			}
		__TEXT_TO_EXPORT += "\n";
	}
}

function exportToCsv(){
var blob = new Blob([__TEXT_TO_EXPORT], { type: 'text/csv;charset=utf-8;' });
	if (navigator.msSaveBlob) { // IE 10+
			navigator.msSaveBlob(blob, __FILE_NAME + ".csv");
	} else {
			var link = document.createElement("a");
			if (link.download !== undefined) { // feature detection
					// Browsers that support HTML5 download attribute
					var url = URL.createObjectURL(blob);
					link.setAttribute("href", url);
					link.setAttribute("download", __FILE_NAME + ".csv");
					link.style = "visibility:hidden";
					document.body.appendChild(link);
					link.click();
					document.body.removeChild(link);
			}
		}
		__EXTRACTING_DATA = false;
}