sap.ui.define([
    "sap/fe/test/JourneyRunner",
	"ns/ordersapp/test/integration/pages/OrdersList",
	"ns/ordersapp/test/integration/pages/OrdersObjectPage",
	"ns/ordersapp/test/integration/pages/OrderItemsObjectPage"
], function (JourneyRunner, OrdersList, OrdersObjectPage, OrderItemsObjectPage) {
    'use strict';

    var runner = new JourneyRunner({
        launchUrl: sap.ui.require.toUrl('ns/ordersapp') + '/test/flp.html#app-preview',
        pages: {
			onTheOrdersList: OrdersList,
			onTheOrdersObjectPage: OrdersObjectPage,
			onTheOrderItemsObjectPage: OrderItemsObjectPage
        },
        async: true
    });

    return runner;
});

