package ro.croco.integration.dms.toolkit.db;
import ro.croco.integration.dms.commons.DatabaseUtils;
import ro.croco.integration.dms.commons.exceptions.IntegrationServiceException;
import ro.croco.integration.dms.commons.validation.StoreServicePropValidator;
import ro.croco.integration.dms.toolkit.IntegrationServiceImpl_Abstract;
import ro.croco.integration.dms.toolkit.StoreContext;
import ro.croco.integration.dms.toolkit.StoreServiceMessage;
import ro.croco.integration.dms.toolkit.StoreServiceMessageEvent;

import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;
import java.util.UUID;

/**
 * Created by Lucian.Dragomir on 1/17/2015.
 */
public class IntegrationServiceImpl_Db extends IntegrationServiceImpl_Abstract {

    private StoreServicePropValidator validator = new StoreServicePropValidator(new ContextProperties.Required());

    @Override
    public void __init(Properties context) throws IOException {
        super.__init(context);
        validator.validate(context);
    }

    @Override
    public StoreServiceMessage[] sendAndReceive(StoreServiceMessage[] messageStructures,StoreContext.COMMUNICATION_TYPE_VALUES communicationType) {
        StoreServiceMessage[] responses = new StoreServiceMessage[messageStructures.length];

        if(communicationType.equals(StoreContext.COMMUNICATION_TYPE_VALUES.SYNCHRONOUS)){
            if(messageStructures.length > 1)
                throw new IntegrationServiceException("Cannot process multiple messages for synchronous mode communication.");

            SyncFrontIntegrationWorker_DB worker = new SyncFrontIntegrationWorker_DB(context);
            StoreServiceMessageDb dbMessage = constructSyncDbMsg(messageStructures[0]);
            worker.send(dbMessage,constructSyncConnection(true),syncConnectionDbSchema(true),QueueConfigurationResolver.getTableName(context,context.getProperty(ContextProperties.Required.SERVICE_SYNC_REQUEST_QUEUE)));
            responses[0] = worker.receive(constructSyncConnection(false),syncConnectionDbSchema(false),QueueConfigurationResolver.getTableName(context,context.getProperty(ContextProperties.Required.SERVICE_SYNC_RESPONSE_QUEUE)),QueueConfigurationResolver.getHistoryTable(context,context.getProperty(ContextProperties.Required.SERVICE_SYNC_RESPONSE_QUEUE)),Long.valueOf(context.getProperty(ContextProperties.Required.SYNCHRONOUS_MESSAGE_WAIT_RESPONSE_TIMEOUT)),Long.valueOf(context.getProperty(ContextProperties.Required.SYNCHRONOUS_MESSAGE_WAIT_RESPONSE_ON_ITERATION))).getStoreServiceMessage();
        }
        return responses;
    }

    @Override
    public StoreServiceMessage sendAndReceive(StoreServiceMessage messageStructure, StoreContext.COMMUNICATION_TYPE_VALUES communicationType) {
        return sendAndReceive(new StoreServiceMessage[]{messageStructure},communicationType)[0];
    }

    @Override
    protected synchronized StoreServiceMessageEvent.StoreServiceMessageProcessStatus fireStoreServiceMessageEvent(StoreServiceMessage storeServiceMessage, boolean requireProcessStatus) {
        return super.fireStoreServiceMessageEvent(storeServiceMessage, requireProcessStatus);
    }

    @Override
    public void close(){

    }

    private StoreServiceMessageDb constructSyncDbMsg(StoreServiceMessage ssMessage){
        StoreServiceMessageDb dbMessage = new StoreServiceMessageDb();
        dbMessage.setStoreServiceMessage(ssMessage);
        dbMessage.setMessageCorrelationID(UUID.randomUUID().toString());
        dbMessage.setMessageExpiration(System.currentTimeMillis() + (Long.valueOf(context.getProperty(ContextProperties.Required.SYNCHRONOUS_MESSAGE_WAIT_RESPONSE_TIMEOUT)) * 1000));
        dbMessage.setMessagePriority(Integer.valueOf((String)context.get(ContextProperties.Required.SYNCHRONOUS_MESSAGE_PRIORITY_DEFAULT)));
        dbMessage.setMessageDestination(context.getProperty(ContextProperties.Required.SERVICE_SYNC_REQUEST_QUEUE));
        dbMessage.setMessageReplyTo(context.getProperty(ContextProperties.Required.SERVICE_SYNC_RESPONSE_QUEUE));
        return dbMessage;
    }

    private Connection constructSyncConnection(boolean onRequest){
        if(onRequest)
            return DatabaseUtils.getConnection(context,QueueConfigurationResolver.getConnectionName(context,context.getProperty(ContextProperties.Required.SERVICE_SYNC_REQUEST_QUEUE)));
        else return DatabaseUtils.getConnection(context,QueueConfigurationResolver.getConnectionName(context,context.getProperty(ContextProperties.Required.SERVICE_SYNC_RESPONSE_QUEUE)));
    }

    private String syncConnectionDbSchema(boolean onRequest){
        if(onRequest)
            return DatabaseUtils.getConnectionSchema(context,context.getProperty(ContextProperties.Required.SERVICE_SYNC_REQUEST_QUEUE));
        else return DatabaseUtils.getConnectionSchema(context,context.getProperty(ContextProperties.Required.SERVICE_SYNC_RESPONSE_QUEUE));
    }

    private Connection constructAsyncConnection(boolean onRequest){
        if(onRequest)
            return DatabaseUtils.getConnection(context,QueueConfigurationResolver.getConnectionName(context,context.getProperty(ContextProperties.Required.SERVICE_ASYNC_REQUEST_QUEUE)));
        else return DatabaseUtils.getConnection(context,QueueConfigurationResolver.getConnectionName(context,context.getProperty(ContextProperties.Required.SERVICE_ASYNC_RESPONSE_QUEUE)));
    }

    private String asyncConnectionDbSchema(boolean onRequest){
        if(onRequest)
            return DatabaseUtils.getConnectionSchema(context,context.getProperty(ContextProperties.Required.SERVICE_ASYNC_REQUEST_QUEUE));
        else return DatabaseUtils.getConnectionSchema(context,context.getProperty(ContextProperties.Required.SERVICE_ASYNC_RESPONSE_QUEUE));
    }
}