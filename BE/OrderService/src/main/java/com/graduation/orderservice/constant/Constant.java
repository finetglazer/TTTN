package com.graduation.orderservice.constant;

public class Constant {

    // ===================== RESPONSE FIELD NAMES =====================
    public static final String RESPONSE_SUCCESS = "success";
    public static final String RESPONSE_MESSAGE = "message";
    public static final String RESPONSE_ORDER = "order";
    public static final String RESPONSE_SAGA_ID = "sagaId";
    public static final String RESPONSE_ERROR = "error";
    public static final String RESPONSE_ORDERS = "orders";
    public static final String RESPONSE_TOTAL_COUNT = "totalCount";
    public static final String RESPONSE_SERVICE = "service";
    public static final String RESPONSE_STATUS = "status";
    public static final String RESPONSE_TIMESTAMP = "timestamp";

    // ===================== SUCCESS MESSAGES =====================
    public static final String ORDER_CREATED_SUCCESS = "Order created successfully";
    public static final String ORDER_CANCELLATION_INITIATED = "Order cancellation initiated";
    // ===================== ERROR MESSAGES =====================
    public static final String ORDER_NOT_FOUND = "Order not found";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String FAILED_TO_CREATE_ORDER = "Failed to create order: ";
    public static final String ERROR_RETRIEVING_ORDER = "Error retrieving order: ";
    public static final String ERROR_RETRIEVING_USER_ORDERS = "Error retrieving user orders: ";
    public static final String ERROR_RETRIEVING_ORDER_STATUS = "Error retrieving order status: ";

    // ===================== DEFAULT VALUES =====================
    public static final String SAGA_ID_PENDING = "pending";

    // ===================== SERVICE INFORMATION =====================
    public static final String SERVICE_NAME = "OrderService";
    public static final String STATUS_RUNNING = "Running";
    public static final String STATUS_UP = "UP";
    public static final String TEST_ENDPOINT_MESSAGE = "Order Service Test Endpoint";

    // ===================== KAFKA COMMAND TYPES =====================
    public static final String COMMAND_ORDER_UPDATE_CONFIRMED = "ORDER_UPDATE_CONFIRMED";
    public static final String COMMAND_ORDER_UPDATE_DELIVERED = "ORDER_UPDATE_DELIVERED";
    public static final String COMMAND_ORDER_CANCEL = "ORDER_CANCEL";

    // ===================== KAFKA EVENT TYPES =====================
    public static final String EVENT_ORDER_CREATED = "ORDER_CREATED";
    public static final String EVENT_ORDER_STATUS_UPDATED_CONFIRMED = "ORDER_STATUS_UPDATED_CONFIRMED";
    public static final String EVENT_ORDER_STATUS_UPDATED_DELIVERED = "ORDER_STATUS_UPDATED_DELIVERED";
    public static final String EVENT_ORDER_STATUS_UPDATE_FAILED = "ORDER_STATUS_UPDATE_FAILED";
    public static final String EVENT_ORDER_CANCELLED = "ORDER_CANCELLED";
    public static final String EVENT_ORDER_CANCELLATION_FAILED = "ORDER_CANCELLATION_FAILED";
    public static final String EVENT_CANCEL_REQUEST_RECEIVED = "CANCEL_REQUEST_RECEIVED";


    // ===================== MAP FIELD NAMES =====================
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_SAGA_ID = "sagaId";
    public static final String FIELD_MESSAGE_ID = "messageId";
    public static final String FIELD_PAYLOAD = "payload";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_ORDER_ID = "orderId";
    public static final String FIELD_USER_ID = "userId";
    public static final String FIELD_USER_EMAIL = "userEmail";
    public static final String FIELD_USER_NAME = "userName";
    public static final String FIELD_ORDER_DESCRIPTION = "orderDescription";
    public static final String FIELD_TOTAL_AMOUNT = "totalAmount";
    public static final String FIELD_ORDER_STATUS = "orderStatus";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_SUCCESS = "success";
    public static final String FIELD_ERROR_MESSAGE = "errorMessage";

    // ===================== VALIDATION MESSAGES =====================
    // Order validation messages
    public static final String VALIDATION_USER_ID_BLANK = "User ID cannot be blank";
    public static final String VALIDATION_USER_EMAIL_BLANK = "User email cannot be blank";
    public static final String VALIDATION_INVALID_EMAIL = "Invalid email format";
    public static final String VALIDATION_USER_NAME_BLANK = "User name cannot be blank";
    public static final String VALIDATION_ORDER_DESCRIPTION_BLANK = "Order description cannot be blank";
    public static final String VALIDATION_ORDER_DESCRIPTION_SIZE = "Order description cannot exceed 1000 characters";
    public static final String VALIDATION_TOTAL_AMOUNT_NULL = "Total amount cannot be null";
    public static final String VALIDATION_TOTAL_AMOUNT_MIN = "Total amount must be greater than 0";
    public static final String VALIDATION_TOTAL_AMOUNT_DIGITS = "Total amount must have at most 10 integer digits and 2 decimal places";
    public static final String VALIDATION_ORDER_STATUS_NULL = "Order status cannot be null";
    public static final String VALIDATION_INVALID_AMOUNT_FORMAT = "Invalid amount format";

    // OrderHistory validation messages
    public static final String VALIDATION_ORDER_ID_NULL = "Order ID cannot be null";
    public static final String VALIDATION_PREVIOUS_STATUS_NULL = "Previous status cannot be null";
    public static final String VALIDATION_NEW_STATUS_NULL = "New status cannot be null";
    public static final String VALIDATION_CHANGED_BY_BLANK = "Changed by cannot be blank";

    // ===================== ORDER STATUS DESCRIPTIONS =====================
    public static final String STATUS_DESC_CREATED = "Order has been created";
    public static final String STATUS_DESC_CONFIRMED = "Order has been confirmed";
    public static final String STATUS_DESC_DELIVERED = "Order has been delivered";
    public static final String STATUS_DESC_CANCELLED = "Order has been cancelled";
    public static final String STATUS_DESC_CANCELLATION_PENDING = "Cancellation in progress";

    // ===================== ORDER BUSINESS LOGIC MESSAGES =====================
    public static final String ERROR_INVALID_STATUS_TRANSITION = "Invalid status transition from %s to %s";
    public static final String ERROR_CANNOT_CHANGE_FINAL_STATUS = "Cannot change status from final state: %s";
    public static final String ERROR_CANNOT_CANCEL_STATUS = "Order cannot be cancelled in current status: %s";
    public static final String ERROR_CAN_ONLY_CONFIRM_CREATED = "Order can only be confirmed from CREATED status, current: %s";
    public static final String ERROR_CAN_ONLY_DELIVER_CONFIRMED = "Order can only be delivered from CONFIRMED status, current: %s";
    public static final String ERROR_INVALID_ORDER_STATUS_CANCELLATION_PENDING = "Order is currently being cancelled";
    public static final String ERROR_PAYMENT_IN_PROGRESS = "Payment is currently being processed. Please wait for payment completion before cancelling.";
    public static final String ERROR_CANCELLATION_NOT_ALLOWED_PAYMENT = "Order cancellation not allowed";
    // ===================== DEFAULT REASONS =====================
    public static final String REASON_ORDER_CONFIRMED = "Order confirmed for processing";
    public static final String REASON_ORDER_DELIVERED = "Order successfully delivered";
    public static final String REASON_ORDER_CONFIRMED_SUCCESS = "Order confirmed successfully";
    public static final String REASON_ORDER_DELIVERED_SUCCESS = "Order delivered successfully";
    public static final String REASON_ORDER_CANCELLED_SAGA = "Order cancelled by saga";

    // ===================== ORDER HISTORY MESSAGES =====================
    public static final String CHANGE_DESC_FORMAT = "Status changed from %s to %s";
    public static final String CHANGE_DESC_WITH_REASON = " - %s";

    // ===================== ACTORS/PERFORMERS =====================
    public static final String ACTOR_SAGA_ORCHESTRATOR = "SAGA_ORCHESTRATOR";
    public static final String ACTOR_SAGA_COMPENSATION = "SAGA_COMPENSATION";

    // ===================== IDEMPOTENCY SERVICE MESSAGES =====================
    public static final String ERROR_MESSAGE_ID_REQUIRED = "messageId is required for idempotency check";
    public static final String ERROR_MESSAGE_ID_REQUIRED_RECORD = "messageId is required";
    public static final String ERROR_SAGA_ID_REQUIRED = "sagaId is required";
    public static final String ERROR_FAILED_TO_RECORD = "Failed to record processing";

    // ===================== CONTROLLER LOG MESSAGES =====================
    public static final String LOG_CREATING_ORDER = "Creating order for user: {} with amount: {}";
    public static final String LOG_ERROR_CREATING_ORDER = "Error creating order for user: {}";
    public static final String LOG_ERROR_GETTING_ORDER = "Error getting order: {}";
    public static final String LOG_ERROR_GETTING_USER_ORDERS = "Error getting orders for user: {}";

    // ===================== KAFKA LISTENER LOG MESSAGES =====================
    public static final String LOG_PROCESSING_ORDER_COMMAND = "Processing order command type: {} for saga: {} messageId: {}";
    public static final String LOG_PROCESSING_CANCEL_ORDER_COMMAND = "Processing order cancellation command type with orderId: {}";

    public static final String LOG_UNKNOWN_ORDER_COMMAND = "Unknown order command type: {} for saga: {}";
    public static final String LOG_ORDER_COMMAND_ACKNOWLEDGED = "Order command acknowledged: {} for saga: {}";
    public static final String LOG_ERROR_PROCESSING_ORDER_COMMAND = "Error processing order command: {}";
    public static final String ERROR_ORDER_COMMAND_PROCESSING_FAILED = "Order command processing failed";

    // ===================== SERVICE LOG MESSAGES =====================
    public static final String LOG_MESSAGE_ALREADY_PROCESSED = "Message already processed: messageId={}, sagaId={}";
    public static final String LOG_MESSAGE_NOT_PROCESSED = "Message not processed: messageId={}, sagaId={}";
    public static final String LOG_RECORDED_PROCESSING = "Recorded processing for messageId: {}, sagaId: {}, status: {}";
    public static final String LOG_FAILED_TO_RECORD_PROCESSING = "Failed to record processing for messageId: {}, sagaId: {}";
    public static final String LOG_CLEANING_OLD_MESSAGES = "Cleaning up old processed messages";
    public static final String LOG_DELETED_OLD_MESSAGES = "Deleted {} old processed messages";
    public static final String LOG_OLD_MESSAGES_CLEANUP_SUCCESS = "Old processed messages cleanup completed successfully";
    public static final String LOG_NO_OLD_MESSAGES = "No old processed messages to clean up";
    public static final String LOG_ORDER_CREATED_SUCCESS = "Order created successfully with ID: {}";
    public static final String LOG_PUBLISHED_ORDER_CREATED = "Published ORDER_CREATED event for order: {} to trigger saga";
    public static final String LOG_FAILED_PUBLISH_ORDER_CREATED = "Failed to publish ORDER_CREATED event for order: {}";
    public static final String LOG_UPDATING_ORDER_STATUS = "Updating order {} status to {} for saga: {}";
    public static final String LOG_ORDER_STATUS_UPDATED = "Order {} status updated to {} successfully";
    public static final String LOG_CANCELLING_ORDER = "Cancelling order {} for saga: {}";
    public static final String LOG_ORDER_CANCELLED_SUCCESS = "Order {} cancelled successfully";
    public static final String LOG_UPDATING_ORDER_CONFIRMED = "Updating order {} to CONFIRMED for saga: {}";
    public static final String LOG_UPDATING_ORDER_DELIVERED = "Updating order {} to DELIVERED for saga: {}";
    public static final String LOG_PROCESSING_DELIVERY_WAIT = "Processing order delivery update for saga: {} - waiting 10 seconds...";
    public static final String LOG_THREAD_INTERRUPTED = "Thread interrupted during delay for saga: {}";
    public static final String LOG_SAGA_COMPLETED = "Saga {} completed successfully - Order {} delivered";
    public static final String LOG_SAGA_FAILED = "Saga {} failed - Order {} delivery update failed: {}";
    public static final String LOG_ERROR_UPDATING_CONFIRMED = "Error updating order to confirmed: {}";
    public static final String LOG_ERROR_UPDATING_DELIVERED = "Error updating order to delivered: {}";
    public static final String LOG_ERROR_CANCELLING_ORDER = "Error cancelling order: {}";
    public static final String LOG_INVALID_ORDER_ID = "Invalid orderId: {} for sagaId: {}";
    public static final String LOG_PUBLISHING_ORDER_EVENT = "Publishing order event: sagaId={}, orderId={}, eventType={}, success={}";
    public static final String LOG_ORDER_EVENT_SUCCESS = "Order event success: {}";
    public static final String LOG_ORDER_EVENT_FAILURE = "Order event failure: {}";
    public static final String LOG_ERROR_PUBLISHING_EVENT = "Error publishing order event: {}";
    public static final String LOG_PAYMENT_LOCK_CHECK = "Checking payment lock for order cancellation: orderId={}, lockKey={}";
    public static final String LOG_PAYMENT_IN_PROGRESS = "Payment in progress detected: orderId={}, lockHolder={}";

    // ===================== BUSINESS ERROR MESSAGES =====================
    public static final String ERROR_ORDER_NOT_FOUND_ID = "Order not found: %s";
    public static final String ERROR_AUTHORIZATION = "You are not authorized to cancel this order";
    public static final String ERROR_FAILED_TO_CREATE_ORDER_RUNTIME = "Failed to create order: %s";
    public static final String ERROR_FAILED_TO_CANCEL_ORDER = "Cancellation not allowed";
    public static final String ERROR_INVALID_ORDER_ID = "Invalid order ID";
    public static final String ERROR_INVALID_ORDER_STATUS_DELIVERED_FOR_CANCELLING = "Cannot cancel order that has already been delivered";
    public static final String ERROR_INVALID_ORDER_STATUS_BE_ALREADY_CANCELLED_FOR_CANCELLING = "Cannot cancel order that has already been cancelled";
    // ===================== KAFKA TOPICS AND PREFIXES =====================
    public static final String TOPIC_ORDER_EVENTS = "order.events";
    public static final String PREFIX_ORDER_MESSAGE = "ORDER_MSG_";
    public static final String PREFIX_ORDER_EVENT = "ORDER_EVT_";

    // ===================== FORMAT STRINGS =====================
    public static final String FORMAT_ORDER_TOSTRING = "Order{id=%d, userId='%s', status=%s, totalAmount=%s, sagaId='%s'}";
    public static final String FORMAT_ORDER_HISTORY_TOSTRING = "OrderHistory{id=%d, orderId=%d, %s->%s, changedBy='%s', changedAt=%s}";
    public static final String FORMAT_CREATE_ORDER_REQUEST_TOSTRING = "CreateOrderRequest{userId='%s', userEmail='%s', userName='%s', totalAmount=%s}";

    // ===================== TABLE AND COLUMN NAMES =====================
    public static final String TABLE_ORDERS = "orders";
    public static final String TABLE_ORDER_HISTORY = "order_history";
    public static final String TABLE_PROCESSED_MESSAGES = "processed_messages";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USER_EMAIL = "user_email";
    public static final String COLUMN_USER_NAME = "user_name";
    public static final String COLUMN_ORDER_DESCRIPTION = "order_description";
    public static final String COLUMN_TOTAL_AMOUNT = "total_amount";
    public static final String COLUMN_SHIPPING_ADDRESS = "shipping_address";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";
    public static final String COLUMN_SAGA_ID = "saga_id";
    public static final String COLUMN_ORDER_ID = "order_id";
    public static final String COLUMN_PREVIOUS_STATUS = "previous_status";
    public static final String COLUMN_NEW_STATUS = "new_status";
    public static final String COLUMN_REASON = "reason";
    public static final String COLUMN_CHANGED_AT = "changed_at";
    public static final String COLUMN_CHANGED_BY = "changed_by";
    public static final String COLUMN_MESSAGE_ID = "message_id";
    public static final String COLUMN_PROCESSED_AT = "processed_at";

    // ===================== INDEX NAMES =====================
    public static final String INDEX_ORDER_USER_ID = "idx_order_user_id";
    public static final String INDEX_ORDER_STATUS = "idx_order_status";
    public static final String INDEX_ORDER_SAGA_ID = "idx_order_saga_id";
    public static final String INDEX_ORDER_CREATED_AT = "idx_order_created_at";
    public static final String INDEX_ORDER_HISTORY_ORDER_ID = "idx_order_history_order_id";
    public static final String INDEX_ORDER_HISTORY_CHANGED_AT = "idx_order_history_changed_at";
    public static final String INDEX_ORDER_HISTORY_CHANGED_BY = "idx_order_history_changed_by";
}