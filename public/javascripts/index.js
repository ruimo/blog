var removeArticle = function(id, confirmMsg, yes, no) {
  remove(id, confirmMsg, yes, no, "#removeArticleForm");
}

var removeComment = function(id, confirmMsg, yes, no) {
  remove(id, confirmMsg, yes, no, "#removeCommentForm");
}

var remove = function(id, confirmMsg, yes, no, formName) {
  var dlg = $("#removeDialog");
  dlg.text(confirmMsg);
  dlg.dialog({
    modal: true,
    buttons: [
      {
        text: yes,
        class: 'yes-button',
        click: function() {
          $(formName + " .id").attr("value", id);
          $(formName).submit();
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

var postComment = function(articleId, form) {
  $.ajax({
    url: $(form).attr("action"),
    method: $(form).attr("method"),
    dataType: "json",
    data: new FormData(form),
    processData: false,
    contentType: false
  }).done(function(data, status, jqXhr) {
    location.href='/';
  }).fail(function(jqXhr, textStatus, errorThrown) {
    console.log('fail', textStatus);
    if (jqXhr.responseJSON.status === "ValidationError") {
      console.log('validation error', JSON.stringify(jqXhr.responseJSON));
      $(form).html(jqXhr.responseJSON.form);
      $("button,input[type='button'],input[type='submit']").button();
    }
  });
};
