$(document).ready(function () {
	var open = false;
	var toggle = $('#sidebar-toggle');
	var sidebar = $('#sidebar');
	var content = $('#content');

	// Toggle Mobile Nav on Nav-Toggle Tap/Click
	toggle.on('click', function(event) {
		toggleMenu(event);
	});

	// Close Mobile Navigaiton on Content Tap/Click
	content.on('click', function(event) {
		if (open && !($(event.target).is(toggle) || $(event.target).is(toggle.find('span')))) {
			toggleMenu(event);
		}
	});

	// close on ESC key
	$(document).on('keydown', function(event) {
		if (open && (event.keyCode === 27)) {
			event.stopPropagation();
			event.preventDefault();
			toggleMenu(event);
			toggle.blur();
		}
	});

	function toggleMenu(event) {
		event.preventDefault();

		// Toggle Open
		open = !open;

		// Toggle Classes
		toggle.toggleClass('open');
		sidebar.toggleClass('open');
		content.toggleClass('open');
	}

        // Set the side bar toggles
        var toggleWrappers = $('.toggle-wrapper');
        // loop over toggles
        toggleWrappers.each(function(){
            var wrapper = $(this);
            // Sets the click function to 
            $(this).on('click', function(event) {
                //event.preventDefault();
                var menu = $(this).children('.toggle-menu').first();
                if (menu.hasClass("height-transition-hidden"))
                    menu.slideDownTransition();
                else
                    menu.slideUpTransition();
            });
        });
});