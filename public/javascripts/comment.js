var confirmPostComment = function(articleId, form, yes, no, confirmMsg, redirectUrl) {
  var dlg = $("#confirmDialog");
  dlg.text(confirmMsg);
  dlg.dialog({
    modal: true,
    buttons: [
      {
        text: yes,
        class: 'yes-button',
        click: function() {
          $.ajax({
            url: $(form).attr("action"),
            method: $(form).attr("method"),
            dataType: "json",
            data: new FormData(form),
            processData: false,
            contentType: false
          }).done(function(data, status, jqXhr) {
            location.href=redirectUrl;
          }).fail(function(jqXhr, textStatus, errorThrown) {
            console.log('fail', textStatus);
            if (jqXhr.responseJSON.status === "ValidationError") {
              console.log('validation error', JSON.stringify(jqXhr.responseJSON));
              $(form).html(jqXhr.responseJSON.form);
              $("button,input[type='button'],input[type='submit']").button();
            }
          });
        }
      },    
      {
        text: no,
        class: 'no-button',
        click: function() {
          $(this).dialog('close');
        }
      }
    ]
  });
};

var postComment = function(articleId, form, yes, no, confirmMsg, redirectUrl) {
  confirmPostComment(articleId, form, yes, no, confirmMsg, redirectUrl);
};
