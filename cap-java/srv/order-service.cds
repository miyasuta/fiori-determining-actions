using { my.order as db } from '../db/schema';

service OrderService {
    @odata.draft.enabled
    entity Orders as projection on db.Orders
    actions {
        action calculateTotalAmount();
    };
    entity OrderItems as projection on db.OrderItems;
}