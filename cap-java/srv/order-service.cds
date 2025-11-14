using { my.order as db } from '../db/schema';

service OrderService {
    @odata.draft.enabled
    entity Orders as projection on db.Orders
    actions {
        action caldulateTotalAmount();
    };
    entity OrderItems as projection on db.OrderItems;
}