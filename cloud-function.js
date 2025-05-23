const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize the Firebase Admin SDK
admin.initializeApp();

// Helper function to safely log objects with circular references
const safeStringify = (obj, indent = 2) => {
    const cache = new Set();
    return JSON.stringify(obj, (key, value) => {
        if (typeof value === 'object' && value !== null) {
            if (cache.has(value)) {
                return '[Circular]';
            }
            cache.add(value);
        }
        return value;
    }, indent);
};

exports.sendNotification = functions.https.onCall(async (data, context) => {
    try {
        // Log the raw data for debugging
        console.log("Function called. Raw data received:", safeStringify(data));
        console.log("Context auth:", context?.auth || 'No context auth');
        
        // Extract notification data and auth information
        let notificationData = data?.data || data;
        let auth = context?.auth;
        
        // If we have auth data in the request, use it
        if (data?.auth) {
            console.log("Using auth from request data");
            auth = auth || data.auth;
        }
        
        // Log authentication status
        console.log("Auth UID:", auth?.uid || 'No auth');
        
        // Check if we have authentication information
        if (!auth || !auth.uid) {
            console.log("No valid authentication information found");
            throw new functions.https.HttpsError(
                'unauthenticated',
                'The function must be called while authenticated.'
            );
        }
        
        console.log("Extracted notification data:", safeStringify(notificationData));
        
        if (!notificationData) {
            throw new functions.https.HttpsError(
                'invalid-argument',
                'No notification data provided.'
            );
        }

        console.log("Authenticated user:", auth.uid);
        
        // Extract the notification data from the nested structure
        console.log("Notification data structure:", safeStringify(notificationData));
        
        // Extract data from either root level or nested in data property
        const notification = notificationData.notification || notificationData.data?.notification || {};
        const messageData = notificationData.data || {};
        let to = notificationData.to || notificationData.data?.to;
        
        // If to is still not found, try to get it from the data object
        if (!to && notificationData.data && notificationData.data.to) {
            to = notificationData.data.to;
        }
        
        // Validate the request
        if (!to) {
            console.error("Missing 'to' field in notification data");
            throw new functions.https.HttpsError(
                'invalid-argument',
                'The function must be called with a "to" field containing the FCM token.',
                { receivedData: notificationData }
            );
        }
        
        // Log the extracted data safely
        console.log("Extracted data - to:", safeStringify(to));
        console.log("Extracted data - notification:", safeStringify(notification));
        console.log("Extracted data - messageData:", safeStringify(messageData));

        // Helper function to ensure all values in an object are strings
        const stringifyValues = (obj) => {
            if (!obj || typeof obj !== 'object') return {};
            const result = {};
            Object.entries(obj).forEach(([key, value]) => {
                if (value !== undefined && value !== null) {
                    result[key] = String(value);
                } else {
                    result[key] = '';
                }
            });
            return result;
        };

        // Extract the sender's name from the data
        const partnerName = messageData?.senderName || 
                           messageData?.data?.senderName || 
                           messageData?.notification?.senderName ||
                           'Your partner';
        
        // Get the notification title and body from the data or use defaults
        const notificationTitle = `Nudge from ${partnerName}`;
        const notificationBody = messageData?.message || 
                               messageData?.notification?.body || 
                               `${partnerName} is thinking of you ❤️`;
        
        console.log('Extracted partner name:', partnerName);
        console.log('Notification title:', notificationTitle);
        console.log('Notification body:', notificationBody);
        console.log('Full message data:', safeStringify(messageData));
        
        // Prepare the data payload, ensuring all values are strings
        const fcmData = stringifyValues({
            // Include all the data fields from the request
            ...messageData,
            // Ensure required fields are set
            type: messageData?.type || 'nudge',
            fromUserId: messageData?.fromUserId || auth.uid || '',
            toUserId: messageData?.toUserId || '',
            // Include the sender's name in the data payload
            senderName: partnerName,
            // Include the notification title and body in the data payload
            title: notificationTitle,
            body: notificationBody,
            // Ensure click_action is set for Android
            click_action: messageData?.click_action || 'FLUTTER_NOTIFICATION_CLICK',
        });
        
        console.log('Prepared FCM data:', JSON.stringify(fcmData, null, 2));
        console.log('Incoming message data:', JSON.stringify(messageData, null, 2));
        
        console.log('Using partner name:', partnerName);
        
        // Construct the message
        const message = {
            token: to,
            notification: {
                title: notificationTitle,
                body: notificationBody
            },
            data: {
                ...fcmData,
                title: notificationTitle,
                body: notificationBody,
                senderName: partnerName  // Ensure senderName is included in data payload
            },
            android: {
                priority: 'high',
                notification: {
                    sound: 'default',
                    defaultSound: true,
                    defaultVibrateTimings: true,
                    defaultLightSettings: true,
                    visibility: 'public',
                    notificationCount: 1,
                    icon: 'ic_notification',
                    color: '#FF4081'
                }
            },
            apns: {
                payload: {
                    aps: {
                        alert: {
                            title: notificationTitle,
                            body: notificationBody
                        },
                        sound: 'default',
                        badge: 1,
                        'mutable-content': 1,
                        'content-available': 1
                    }
                }
            },
            webpush: {
                headers: {
                    'Urgency': 'high',
                    'TTL': '2419200'
                },
                notification: {
                    title: notificationTitle,
                    body: notificationBody,
                    icon: 'https://your-app-url/icon.png',
                    badge: 'https://your-app-url/badge.png',
                    vibrate: [200, 100, 200],
                    requireInteraction: true,
                    actions: [{
                        action: 'view',
                        title: 'Open App'
                    }]
                },
                fcmOptions: messageData?.click_action ? {
                    link: messageData.click_action
                } : undefined
            }
        };
        
        // Add click_action for web push if needed
        if (messageData?.click_action) {
            message.webpush.fcm_options = {
                link: messageData.click_action
            };
        }
        
        console.log("Constructed message:", safeStringify(message));

        const response = await admin.messaging().send(message);
        console.log('Successfully sent message:', safeStringify(response));
        return { messageId: response };
    } catch (error) {
        console.error("Error in sendNotification function:", error);
        console.error("Error stack:", error.stack);
        throw new functions.https.HttpsError(
            'internal',
            error.message || 'An error occurred while processing the request',
            { error: error.toString(), stack: error.stack }
        );
    }
});
