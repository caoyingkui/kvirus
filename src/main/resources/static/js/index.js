var log_editor;
var diff_editor;
var stompClient;

function initailaize(){
    log_editor_initialize();
    diff_editor_initialize();
    connect();


}

function connect(){
    var socket = new SockJS('/gs-guide-websocket')
    stompClient = Stomp.over(socket)
    stompClient.connect({}, function(frame){
        stompClient.subscribe("/message/logs", update_log_editor());
        stompClient.subscribe("/message/diffs", update_diff_files());
    });
}

function log_editor_initialize(){
    //这里的路径好奇怪，好像是按照html的位置进行寻址的，而不是js
    require.config({paths: {"ace": "lib/ace"}});
    // load ace and extensions
    require(["ace/ace"], function (ace) {
        log_editor = ace.edit("log_editor");
        log_editor.setOptions({
            autoScrollEditorIntoView: true,
            maxLines: 50,
            minLines:1
        });
        console.log("in");
        log_editor.renderer.setScrollMargin(10, 10, 10, 10);
        log_editor.setHighlightActiveLine(false);
        //log_editor.session.selection.on("changeCursor", onEditor);
        log_editor.setValue("code to search!");
        //code_editor.session.addMarker(new Range(1,0, 3, 2), "ace_active-line", "fullLine");
    });
}

function diff_editor_initialize(){
    var diff_editor = new AceDiff({
        element: '.acediff',
        left: {
            content: 'your first file content here',
        },
        right: {
            content: 'your second file content here',
        },
    });
}

function update_log_editor(logs){
    log_editor.setValue(logs);
}

function update_diff_files(files){
    var former = files["former"];
    var latter = files["latter"];
    left.content = former;
    right.content = latter;
}

