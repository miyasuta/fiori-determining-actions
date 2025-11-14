namespace my.order;
using { managed } from '@sap/cds/common';

entity Orders : managed {
    key ID : UUID;
    customerName : String(100);
    oderDate : Date;
    totalAmount : Decimal(10,2);
    items: Composition of many OrderItems on items.orderID = $self;
    
}

entity OrderItems : managed {
    key ID : UUID;
    orderID : Association to Orders;
    productName : String(100);
    quantity : Integer;
    unitPrice : Decimal(10,2);
}