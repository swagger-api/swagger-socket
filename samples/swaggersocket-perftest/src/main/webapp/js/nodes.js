
var redraw, g, renderer;

function addEdge(start, end) {
	g.addEdge(start, end);
}

function addNode(name) {
	g.addNode(name);
	redraw();
}

var bang;
function setup() {
	if(g == null) g = new Graph();
	var width = $(document).width() - 20;
	var height = $(document).height() - 200;
    var st = {
    	directed: true
    };
	var renderer = new Graph.Renderer.Raphael('canvas', g, width, height);
	var layouter = new Graph.Layout.Spring(g);

    redraw = function() {
        layouter.layout();
        renderer.draw();
    };
    hide = function(id) {
        g.nodes[id].hide();
    };
    show = function(id) {
        g.nodes[id].show();
    };
    bang = function(id) {
    	for(var prop in g.nodes) {
    	    g.removeNode(prop.id);
    	}
//    	g.nodes.forEach(function (node){
//    		node.hide();
//    	})
//    	g.nodes = Array();
    	redraw();
    };
};