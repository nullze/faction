
require('suneditor/dist/css/suneditor.min.css');
require('../scripts/fileupload/css/fileinput.css');
require('../loading/css/jquery-loading.css');
//require('bootstrap/dist/css/bootstrap.css');
import suneditor from 'suneditor';
import {font, fontColor, fontSize, align, image, imageGallery, list, formatBlock, table, blockquote } from 'suneditor/src/plugins';
import CodeMirror from 'codemirror';
import 'codemirror/mode/htmlmixed/htmlmixed';
import 'codemirror/lib/codemirror.css';
import '../loading/js/jquery-loading';
import 'jquery';
import 'datatables.net';
import 'datatables.net-bs'   ;
import '../scripts/fileupload/js/fileinput.min';
import 'bootstrap';
import 'jquery-ui';
import 'jquery-confirm';
import 'select2';
import '../scripts/jquery.autocomplete.min';

    global.deleteStatus = function deleteStatus(status){
    	let data = "status=" +status;
		data +="&_token=" + _token;
		$.post("deleteStatus",data).done(function(resp){
			alertRedirect(resp);
		});
    };

   	$(function(){
        $("input").each((index,el) => {if(el.type == "checkbox"){$(el).addClass("icheckbox_minimal-blue");}});
        $("#cfType").select2();
   	
   		$('.statuscheck').on('click', function(event){
            $(".statuscheck").each( (index, el)  => { $(el).removeAttr("checked");});
   			console.log($(this).closest('td').attr("status"));
   			let data="status=" + $(this).closest('td').attr("status");
   			data+="&_token="+ _token;
   			$.post("setDefaultStatus",data).done(function(resp){
   				alertMessage(resp, "Status Updated");
   			});
	
   		});
   		
   		$("#addstatus").click(function(){
   			let data = "status=" +$("#asmtStatus").val();
   			data +="&_token=" + _token;
   			$.post("createStatus",data).done(function(resp){
   				alertRedirect(resp);
   			});
   		});
   		
        var editorOptions = {
            codeMirror: CodeMirror,
            plugins: [font, fontColor, fontSize, image, align, imageGallery, list, formatBlock, table, blockquote],
            buttonList : [
            ['undo', 'redo', 'font', 'fontSize', 'formatBlock'],
            ['bold', 'underline', 'italic', 'strike', 'subscript', 'superscript', 'removeFormat'],
            ['fontColor', 'hiliteColor', 'outdent', 'indent', 'align', 'horizontalRule', 'list', 'table'],
            ['link', 'image', 'fullScreen', 'showBlocks', 'codeView', 'preview'],
            
            ],
            defaultStyle: 'font-family: arial; font-size: 18px',
            height: 500
        };
        var emailSignature = suneditor.create("emailSignature", editorOptions);

		$("#type").DataTable();
		$("#campaign").DataTable();
		$("#addType").click(function(){
			let name = $("#typeName").val();
			let data="action=addType";
			data+="&name=" + name;
			data+="&_token=" + _token;
			$.post("Options",data).done(function(resp){
				alertRedirect(resp);
			});
		});
		
		
		
		$("#addCampaign").click(function(){
			let name = $("#campaignName").val();
			let data="action=addCampaign";
			data+="&name=" + name;
			data+="&_token=" + _token;
			$.post("Options",data).done(function(resp){
				alertRedirect(resp);
			});
		});
		
		$("#testEmail").click(function(){
			$.confirm({
				title: "Send Test Email",
				theme: "black",
				content : "<div class=row><div class=col-md-3><label for=to >Send TO:</label></div><div class=col-md-9><input name=to style='color:black; width:100%' type=text id=to /></div></div>",
				buttons: {
					confirm : function (){
						
						let ssl=$("#isSSL").is(":checked");
						let tls=$("#isTLS").is(":checked");
						let auth=$("#isAuth").is(":checked");
						let data="action=test";
						data+="&server=" + $("#emailServer").val();
						data+="&type="+ $("#emailProto").val();
						data+="&username="+ $("#emailName").val();
						data+="&fromaddress="+ $("#fromAddress").val();
						data+="&port="+ $("#emailPort").val();
						data+="&password="+ encodeURIComponent($("#emailPass").val());
						data+="&prefix="+ $("#emailPrefix").val();
						data+="&signature="+ encodeURIComponent(emailSignature.getContents());
						data+="&sslischecked="+ssl;
						data+="&tlsischecked="+tls;
						data+="&authischecked="+auth;
						data+="&to="+$("#to").val();
						data+="&_token=" + _token;
						$.post("Options", data).done(function (resp){
							alertRedirect(resp);
						});
					}
				}
			});
			
		});
		$("#saveEmail").click(function(){
			let ssl=$("#isSSL").is(":checked");
			let tls=$("#isTLS").is(":checked");
			let auth=$("#isAuth").is(":checked");
			let data="action=emailSettings";
			data+="&server=" + $("#emailServer").val();
			data+="&type="+ $("#emailProto").val();
			data+="&username="+ $("#emailName").val();
			data+="&fromaddress="+ $("#fromAddress").val();
			data+="&port="+ $("#emailPort").val();
			data+="&password="+ encodeURIComponent($("#emailPass").val());
			data+="&prefix="+ $("#emailPrefix").val();
			data+="&signature="+ encodeURIComponent(emailSignature.getContents());
			data+="&sslischecked="+ssl;
			data+="&tlsischecked="+tls;
			data+="&authischecked="+auth;
			data+="&_token=" + _token;
			$.post("Options",data).done(function(resp){
				alertRedirect(resp);
			});
		});
		
		$("#saveOAuth").click(function(){
			let data="server=" + $("#ssoserver").val();
			data+="&username=" + $("#ssousername").val();
			if($("#isSSO").val() == "on")
				data+="&useSSO=true";
			else
				data+="&useSSO=false";
			data+="&clientid=" + $("#clientid").val();
			data+="&secret=" + $("#secret").val();
			data+="&profile=" + $("#profile").val();
			data+="&tokenURL=" + $("#tokenurl").val();
			data+="&userURL=" + $("#userurl").val();
			data+="&_token=" + _token;
			$.post("UpdateSSO",data).done(function(resp){
				alertMessage(resp,"SSO Config Updated Successfully");
			});
		});
		
		


   });
   	
   	
	global.editType = function editType(el, typeId){
		var typeName = $($($($(el).parent()).parent()).find("td")[0]).text();
		$.confirm({
			title: "Editing Assessment Type",
			content: "<input id='editTypeName' style='width:100%' value='" + typeName+ "'></input>",
			buttons: {
				"Yes Update" : function(){
					
					let data="id=" + typeId;
					data+="&name=" + $("#editTypeName").val();
					data+="&_token=" + _token;
					$.post("editType",data).done(function(resp){
						alertRedirect(resp);
					});
				},
				cancel:function(){return;}
			}
		});
	};

	global.delType = function delType(el, id){
		
		$.confirm({
			title: "Are you Sure?",
			content: "Deleting this <b>Assessment Type</b> is not reversable.",
			buttons: {
				"Yes Delete" : function(){
					
					let data="action=delType";
					data+="&id=" + id;
					data+="&_token=" + _token;
					$.post("Options",data).done(function(resp){
						alertRedirect(resp);
					});
				},
				cancel:function(){return;}
			}
		});
		

	};
	global.editCampaign = function editCampaign(el, typeId){
		var campName = $($($($(el).parent()).parent()).find("td")[0]).text();
		
		$.confirm({
			title: "Editing Campaign Type",
			content: "<input id='editCampName' style='width:100%' value='" + campName + "'></input>",
			buttons: {
				"Yes Update" : function(){
					
					let data="id=" + typeId;
					data+="&name=" + $("#editCampName").val();
					data+="&_token=" + _token;
					$.post("editCamp",data).done(function(resp){
						alertRedirect(resp);
					});
				},
				cancel:function(){return; }
			}
		});
	};
	
	//$("[id^=delCampaign]").click(
	global.delCampaign = function delCampaign(el, id){
		//var id = $(this).attr("id").replace("delCampaign","");
		$.confirm({
			title: "Are you Sure?",
			content: "Deleting this <b>Campaign</b> is not reversable.",
			buttons: {
				"Yes Delete" : function(){
					
					let data="action=delCampaign";
					data+="&id=" + id;
					data+="&_token=" + _token;
					$.post("Options",data).done(function(resp){
						alertRedirect(resp);
					});
				},
				cancel:function(){return; }
			}

		});
	};
    
    $(function(){
    	$("#addCF").click(function(){
    		let data="cftype=" + $("#cfType").val();
    		data+="&cfname="+$("#cfName").val();
    		data+="&readonly="+$("#readonly").is(":checked");
    		data+="&cfvar="+$("#cfVar").val();
    		data+="&_token=" + _token;
    		$.post("CreateCF", data).done(function(resp){
    			alertRedirect(resp);
    		});
    		
    	});
    	$(".updCF").click(function(){
    		let cfid = $(this).attr("for");
    		let variable = $("#var" + cfid).val();
    		let text = $("#key"+cfid).val();
    		let data = "cfid=" + cfid;
    		data+="&cfname=" + text;
    		data+="&cfvar=" + variable;
    		data+="&readonly="+$("#ro"+cfid).is(":checked");
    		data+="&_token=" + _token;
    		$.post("UpdateCF",data).done(function(resp){
    			alertMessage(resp,"Custom Field Updated");
    			
    		});
    		
    	});
    	$(".delCF").click(function(){
    		var cfid = $(this).attr("for");
    		$.confirm({
				title: "Are you Sure?",
				content: "Deleting this <b>Custom Field</b> is not reversable.",
				buttons: {
					"Yes Delete" : function(){
						
			    		
			    		let data = "cfid=" + cfid;
			    		data+="&_token=" + _token;
			    		$.post("DeleteCF",data).done(function(resp){
			    			alertRedirect(resp);
			    		});
					},
				cancel: function(){return;}
			}
    		});

    	});
    	
    	$("#prEnabled").on("change",function(){
    		
    		let data="prChecked=" + $("#prEnabled").is(":checked");
    		data+="&_token=" + _token;
    		$.post("updatePrConfig", data).done(function(resp){
				if(resp.message){
					$("#prEnabled").prop("checked", false)
				}
    			alertMessage(resp,"PR Config Updated");
				
    		});
    	});
    	$("#feedEnabled").on("change",function(){
    	
    		let data="feedChecked=" + $("#feedEnabled").is(":checked");
    	    data+="&_token=" + _token;
    		$.post("updateFeedConfig", data).done(function(resp){
    			alertMessage(resp,"Feed Setting Updated");
    			
    		});
    	});
    	$("#randEnabled").on("change",function(){
        	
    		let data="randChecked=" + $("#randEnabled").is(":checked");
    		data+="&_token=" + _token;
    		$.post("updateRandConfig", data).done(function(resp){
    			alertMessage(resp,"Application ID Behaviour Updated");
    			
    		});
    	});
    	
    	$("#updateTitles").click(function(){
    		let data="title[0]=" + $("#title1").val();
    		data+="&title[1]=" + $("#title2").val();
    		data+="&_token=" + _token;
    		$.post("updateTitles", data).done(function(resp){
    			alertMessage(resp,"Titles Updated");
    			
    		});
    		
    	});
   	});