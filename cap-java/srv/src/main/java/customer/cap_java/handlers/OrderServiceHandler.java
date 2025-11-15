package customer.cap_java.handlers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnAnalyzer;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsEvent;
import com.sap.cds.services.ErrorStatuses;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;

import cds.gen.orderservice.OrderItems;
import cds.gen.orderservice.OrderItems_;
import cds.gen.orderservice.OrderService_;
import cds.gen.orderservice.Orders;
import cds.gen.orderservice.Orders_;
import cds.gen.orderservice.OrdersCalculateTotalAmountContext;

@Component
@ServiceName(OrderService_.CDS_NAME)
public class OrderServiceHandler implements EventHandler {

    private final PersistenceService db;
    private static final Logger logger = LoggerFactory.getLogger(OrderServiceHandler.class);

    public OrderServiceHandler(PersistenceService db) {
        this.db = db;
    }

    @On(entity = Orders_.CDS_NAME)
    public void onCalculateTotalAmount(OrdersCalculateTotalAmountContext context) {
        // Get the order ID from the context using CqnAnalyzer
        CqnAnalyzer analyzer = CqnAnalyzer.create(context.getModel());
        Map<String, Object> keys = analyzer.analyze(context.getCqn().ref()).targetKeys();
        String orderId = (String) keys.get(Orders.ID);

        DraftService orderDraftService = (DraftService) context.getService();

        // Query the draft order with its items (IsActiveEntity = false for drafts)
        CqnSelect selectOrder = Select.from(Orders_.class)
            .columns(o -> o._all(), o -> o.items().expand())
            .where(o -> o.ID().eq(orderId).and(o.IsActiveEntity().eq(false)));

        Result result = orderDraftService.run(selectOrder);
        Orders order = result.single(Orders.class);

        List<OrderItems> items = order.getItems();

        if (items == null || items.isEmpty()) {
            throw new ServiceException(ErrorStatuses.BAD_REQUEST, "No items found for this order");
        }

        // Validate all items first
        for (int i = 0; i < items.size(); i++) {
            OrderItems item = items.get(i);
            Integer quantity = item.getQuantity();
            BigDecimal unitPrice = item.getUnitPrice();

            if (quantity == null) {
                context.getMessages().error("Quantity is not set for item: " + item.getProductName())
                // .target("items(" + i + ")/quantity");
                .target(Orders_.class, o -> o
                    .items(it -> it.ID().eq(item.getId())
                        .and(it.IsActiveEntity().eq(false)))  // Draftモードの場合
                    .quantity()
                );
            }

            if (unitPrice == null) {
                context.getMessages().error("Unit price is not set for item: " + item.getProductName())
                // .target("items(" + i + ")/unitPrice");
                                .target(Orders_.class, o -> o
                    .items(it -> it.ID().eq(item.getId())
                        .and(it.IsActiveEntity().eq(false)))  // Draftモードの場合
                        .unitPrice()
                );
            }
        }

        // If there are any validation errors, stop processing
        context.getMessages().throwIfError();

        // Calculate total amount (only if validation passed)
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItems item : items) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);
        }

        // Update the draft order's totalAmount
        Orders updatedOrder = Orders.create();
        updatedOrder.setId(orderId);
        updatedOrder.setTotalAmount(totalAmount);

        // log updated total amount
        logger.info("Updating draft order ID {} with total amount: {}", orderId, totalAmount);

        orderDraftService.run(Update.entity(Orders_.class)
            .data(updatedOrder)
            .where(o -> o.ID().eq(orderId).and(o.IsActiveEntity().eq(false))));

        context.setCompleted();
    }

}
