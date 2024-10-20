/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.alarm.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opennms.core.utils.InetAddressUtils.addr;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.spring.BeanUtils;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.dao.DatabasePopulator;
import org.opennms.netmgt.dao.api.AlarmDao;
import org.opennms.netmgt.dao.api.AlarmRepository;
import org.opennms.netmgt.dao.api.CategoryDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsCategory;
import org.opennms.netmgt.model.OnmsDistPoller;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsEventParameter;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsSeverity;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.opennms.web.alarm.AcknowledgeType;
import org.opennms.web.alarm.AlarmQueryParms;
import org.opennms.web.alarm.AlarmUtil;
import org.opennms.web.alarm.SortStyle;
import org.opennms.web.filter.Filter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations= {
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-mockConfigManager.xml",
        "classpath*:/META-INF/opennms/component-dao.xml",
        "classpath*:/META-INF/opennms/component-service.xml",
        "classpath:/daoWebRepositoryTestContext.xml",
        "classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml"
})
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
@DirtiesContext // XXX needed? JUnitTemporaryDatabase marks dirty by default
public class AlarmRepositoryFilterIT implements InitializingBean {

    @Autowired
    DatabasePopulator m_dbPopulator;
    
    @Autowired
    AlarmRepository m_daoAlarmRepo;

    @Autowired
    CategoryDao m_categoryDao;

    @Autowired
    NodeDao m_nodeDao;

    @Autowired
    ApplicationContext m_appContext;
    
    @Override
    public void afterPropertiesSet() throws Exception {
        BeanUtils.assertAutowiring(this);
    }
    
    @Before
    public void setUp(){
        m_dbPopulator.populateDatabase();
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testAlarmTypeFilter(){
        OnmsAlarm[] alarm = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(new AlarmCriteria(new AlarmTypeFilter(3))));
        assertEquals(0, alarm.length);
        
        alarm = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(new AlarmCriteria(new AlarmTypeFilter(1))));
        assertEquals(1, alarm.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testBeforeFirstEventTimeFilter(){
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(new AlarmCriteria(new BeforeFirstEventTimeFilter(new Date()))));
        assertEquals(1, alarms.length);

        alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(new AlarmCriteria(new BeforeLastEventTimeFilter(new Date()))));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testBeforeLastEventTime(){
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(new AlarmCriteria(new BeforeLastEventTimeFilter(new Date()))));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testExactUeiFilter(){
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(new AlarmCriteria(new ExactUEIFilter("test uei"))));
        assertEquals(0, alarms.length);
        
        alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(new AlarmCriteria(new ExactUEIFilter("uei.opennms.org/test"))));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testInterfaceFilter(){
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(new AlarmCriteria(new InterfaceFilter(addr("192.168.1.1")))));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testNegativeAcknowledgeByFilter(){
        AlarmCriteria criteria = new AlarmCriteria(new NegativeAcknowledgedByFilter("non user"));
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testIPLikeFilter(){
        AlarmCriteria criteria = new AlarmCriteria(new IPAddrLikeFilter("192.168.1.1"));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
        assertEquals(addr("192.168.1.1"), alarms[0].getIpAddr());
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testNegativeInterfaceFilter(){
        AlarmCriteria criteria = new AlarmCriteria(new NegativeInterfaceFilter(addr("192.168.1.101")));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testNegativeNodeFilter(){
        AlarmCriteria criteria = getCriteria(new NegativeNodeFilter(11, m_appContext));
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
        
        NegativeNodeFilter filter = new NegativeNodeFilter(11, m_appContext);
        assertEquals("node is not 11", filter.getTextDescription());
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testNegativeExactUeiFilter(){
        AlarmCriteria criteria = getCriteria(new NegativeExactUEIFilter("uei.opennms.org/bogus"));
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testNegativePartialUEIFilter(){
        AlarmCriteria criteria = getCriteria(new NegativePartialUEIFilter("uei.opennms.org"));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(0, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testNegativeServiceFilter(){
        AlarmCriteria criteria = getCriteria(new NegativeServiceFilter(12, null));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testNegativeSeverityFilter(){
        AlarmCriteria criteria = getCriteria(new NegativeSeverityFilter(OnmsSeverity.CRITICAL));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
    }

    /*
    @Ignore
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testNodeFilter(){
        AlarmCriteria criteria = getCriteria(new NodeFilter(1));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
        
        criteria = getCriteria(new NodeFilter(100));
        
        alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(0, alarms.length);
    }
    */

    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testNodeNameLikeFilter(){
        AlarmCriteria criteria = getCriteria(new NodeNameLikeFilter("mr"));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(0, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testSeverityBetweenFilter(){
        AlarmCriteria criteria = getCriteria(new SeverityBetweenFilter(OnmsSeverity.CLEARED, OnmsSeverity.MAJOR));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testServiceFilter(){
        AlarmCriteria criteria = getCriteria(new ServiceFilter(1, null));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testAfterFirstEventTime(){
        AlarmCriteria criteria = getCriteria(new AfterFirstEventTimeFilter(new Date()));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(0, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testDescriptionSubstringFilter(){
        AlarmCriteria criteria = getCriteria(new DescriptionSubstringFilter("alarm"));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testLogMessageSubstringFilter(){
        AlarmCriteria criteria = getCriteria(new LogMessageSubstringFilter("this is a test"));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    @SuppressWarnings("deprecation")
    public void testLogMessageMatchAnyFilter(){
        AlarmCriteria criteria = getCriteria(new LogMessageMatchesAnyFilter("log"));
        
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
    }

    @Test
    @Transactional
    @JUnitTemporaryDatabase
    @SuppressWarnings("deprecation")
    public void testCategoryFilter() {
        // get all alarms
        final Set<OnmsAlarm> allAlarms = Arrays.stream(m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(new AlarmCriteria(new Filter[0])))).collect(Collectors.toSet());

        // create node to categories map
        final Map<Integer, Set<String>> nodeCategoriesMap = new TreeMap<>();

        // iterate over all nodes
        for (final OnmsNode onmsNode : m_nodeDao.findAll()) {
            // get node's categories
            nodeCategoriesMap.put(onmsNode.getId(), onmsNode.getCategories().stream().map(c -> c.getName()).collect(Collectors.toSet()));
        }

        // now iterate over all cetegories
        final List<OnmsCategory> onmsCategories = m_categoryDao.findAll();
        for (final OnmsCategory alarmFound : onmsCategories) {
            // get alarms for given category
            final AlarmCriteria criteria = getCriteria(new CategoryFilter(alarmFound.getName()));
            final OnmsAlarm[] alarmsByCategory = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
            // check that each alarm has a node with the given category associated
            for (final OnmsAlarm onmsAlarm : alarmsByCategory) {
                assertNotNull(onmsAlarm.getNodeId());
                assertTrue(nodeCategoriesMap.get(onmsAlarm.getNodeId()).contains(alarmFound.getName()));
                // remote from all alarms
                allAlarms.remove(onmsAlarm);
            }
        }

        // check remaining alarms
        for (final OnmsAlarm remainingAlarm : allAlarms) {
            // no node or no categories assigned
            assertTrue(remainingAlarm.getNodeId() == null || nodeCategoriesMap.get(remainingAlarm.getNodeId()).isEmpty());
        }
    }

    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testParmsLikeFilter() {
        OnmsDistPoller poller = m_dbPopulator.getDistPollerDao().whoami();
        assertNotNull(poller);

        final OnmsEvent event1 = new OnmsEvent();
        event1.setDistPoller(poller);
        event1.setEventCreateTime(new Date(1436881548292L));
        event1.setEventTime(new Date(1436881548292L));
        event1.setEventCreateTime(new Date(1437061537126L));
        event1.setEventDescr("This is the description of a test event.");
        event1.setEventDisplay("Y");
        event1.setEventHost("127.0.0.1");
        event1.setEventLog("Y");
        event1.setEventLogMsg("Test Event Log Message");
        event1.setEventSeverity(1);
        event1.setEventSource("test");
        event1.setEventTime(new Date(1437061537105L));
        event1.setEventUei("uei.opennms.org/test");
        event1.setIpAddr(InetAddressUtils.getInetAddress("192.168.1.1"));
        event1.setNode(m_dbPopulator.getNode1());
        event1.setServiceType(m_dbPopulator.getServiceTypeDao().findByName("ICMP"));
        event1.setEventParameters(Lists.newArrayList(
                new OnmsEventParameter(event1, "url", "http://localhost:8980/opennms/rtc/post/Network+Interfaces", "string"),
                new OnmsEventParameter(event1, "user", "rtc", "string"),
                new OnmsEventParameter(event1, "passwd", "rtc", "string"),
                new OnmsEventParameter(event1, "catlabel", "Network Interfaces", "string")));
        m_dbPopulator.getEventDao().saveOrUpdate(event1);
        m_dbPopulator.getEventDao().flush();

        final OnmsEvent event2 = new OnmsEvent();
        event2.setDistPoller(poller);
        event2.setEventCreateTime(new Date(1436881548292L));
        event2.setEventTime(new Date(1436881548292L));
        event2.setEventCreateTime(new Date(1437061537126L));
        event2.setEventDescr("This is the description of a test event.");
        event2.setEventDisplay("Y");
        event2.setEventHost("127.0.0.1");
        event2.setEventLog("Y");
        event2.setEventLogMsg("Test Event Log Message");
        event2.setEventSeverity(1);
        event2.setEventSource("test");
        event2.setEventTime(new Date(1437061537105L));
        event2.setEventUei("uei.opennms.org/test");
        event2.setIpAddr(InetAddressUtils.getInetAddress("192.168.1.1"));
        event2.setNode(m_dbPopulator.getNode1());
        event2.setServiceType(m_dbPopulator.getServiceTypeDao().findByName("ICMP"));
        event2.setEventParameters(Lists.newArrayList(
                new OnmsEventParameter(event2, "componentType", "serviceElement", "string"),
                new OnmsEventParameter(event2, "url", "http://localhost:8980/opennms/rtc/post/Network+Interfaces", "string"),
                new OnmsEventParameter(event2, "user", "rtcbomb", "string"),
                new OnmsEventParameter(event2, "passwd", "rtc", "string"),
                new OnmsEventParameter(event2, "catlabel", "Network Interfaces", "string")));
        m_dbPopulator.getEventDao().saveOrUpdate(event2);
        m_dbPopulator.getEventDao().flush();
        
        OnmsAlarm alarm = new OnmsAlarm();
        alarm.setUei("uei.opennms.org/vendor/Juniper/traps/jnxVpnIfUp");
        alarm.setLastEvent(event1);
        alarm.setSeverityId(3);
        alarm.setDistPoller(poller);
        alarm.setCounter(100);
        alarm.setLastEvent(event1);
        
        AlarmDao alarmDao = m_dbPopulator.getAlarmDao();
        alarmDao.save(alarm);
        alarmDao.flush();
        
        OnmsAlarm alarm2 = new OnmsAlarm();
        alarm2.setUei("uei.opennms.org/vendor/Juniper/traps/jnxVpnIfUp");
        alarm2.setLastEvent(event2);
        alarm2.setSeverityId(3);
        alarm2.setDistPoller(poller);
        alarm2.setCounter(100);
        alarm2.setLastEvent(event2);
        
        alarmDao.save(alarm2);
        alarmDao.flush();
        
        EventParmLikeFilter eventParmFilter = new EventParmLikeFilter("user=rtc");
        assertEquals("user=\"rtc\"", eventParmFilter.getTextDescription());
        AlarmCriteria criteria = new AlarmCriteria(eventParmFilter);
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        assertEquals(1, alarms.length);
    }
    
    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testParmsNotLikeFilter() {
        OnmsDistPoller poller = m_dbPopulator.getDistPollerDao().whoami();
        assertNotNull(poller);

        final OnmsEvent event1 = new OnmsEvent();
        event1.setDistPoller(poller);
        event1.setEventCreateTime(new Date(1436881548292L));
        event1.setEventTime(new Date(1436881548292L));
        event1.setEventCreateTime(new Date(1437061537126L));
        event1.setEventDescr("This is the description of a test event.");
        event1.setEventDisplay("Y");
        event1.setEventHost("127.0.0.1");
        event1.setEventLog("Y");
        event1.setEventLogMsg("Test Event Log Message");
        event1.setEventSeverity(1);
        event1.setEventSource("test");
        event1.setEventTime(new Date(1437061537105L));
        event1.setEventUei("uei.opennms.org/test");
        event1.setIpAddr(InetAddressUtils.getInetAddress("192.168.1.1"));
        event1.setNode(m_dbPopulator.getNode1());
        event1.setServiceType(m_dbPopulator.getServiceTypeDao().findByName("ICMP"));
        event1.setEventParameters(Lists.newArrayList(
                new OnmsEventParameter(event1, "url", "http://localhost:8980/opennms/rtc/post/Network+Interfaces", "string"),
                new OnmsEventParameter(event1, "user", "rtc", "string"),
                new OnmsEventParameter(event1, "passwd", "rtc", "string"),
                new OnmsEventParameter(event1, "catlabel", "Network Interfaces", "string")));
        m_dbPopulator.getEventDao().saveOrUpdate(event1);
        m_dbPopulator.getEventDao().flush();

        final OnmsEvent event2 = new OnmsEvent();
        event2.setDistPoller(poller);
        event2.setEventCreateTime(new Date(1436881548292L));
        event2.setEventTime(new Date(1436881548292L));
        event2.setEventCreateTime(new Date(1437061537126L));
        event2.setEventDescr("This is the description of a test event.");
        event2.setEventDisplay("Y");
        event2.setEventHost("127.0.0.1");
        event2.setEventLog("Y");
        event2.setEventLogMsg("Test Event Log Message");
        event2.setEventSeverity(1);
        event2.setEventSource("test");
        event2.setEventTime(new Date(1437061537105L));
        event2.setEventUei("uei.opennms.org/test");
        event2.setIpAddr(InetAddressUtils.getInetAddress("192.168.1.1"));
        event2.setNode(m_dbPopulator.getNode1());
        event2.setServiceType(m_dbPopulator.getServiceTypeDao().findByName("ICMP"));
        event2.setEventParameters(Lists.newArrayList(
                new OnmsEventParameter(event2, "componentType", "serviceElement", "string"),
                new OnmsEventParameter(event2, "url", "http://localhost:8980/opennms/rtc/post/Network+Interfaces", "string"),
                new OnmsEventParameter(event2, "user", "rtcbomb", "string"),
                new OnmsEventParameter(event2, "passwd", "rtc", "string"),
                new OnmsEventParameter(event2, "catlabel", "Network Interfaces", "string")));
        m_dbPopulator.getEventDao().saveOrUpdate(event2);
        m_dbPopulator.getEventDao().flush();
        
        OnmsAlarm alarm = new OnmsAlarm();
        alarm.setUei("uei.opennms.org/vendor/Juniper/traps/jnxVpnIfUp");
        alarm.setLastEvent(event1);
        alarm.setSeverityId(3);
        alarm.setDistPoller(poller);
        alarm.setCounter(100);
        alarm.setLastEvent(event1);
        
        AlarmDao alarmDao = m_dbPopulator.getAlarmDao();
        alarmDao.save(alarm);
        alarmDao.flush();
        
        OnmsAlarm alarm2 = new OnmsAlarm();
        alarm2.setUei("uei.opennms.org/vendor/Juniper/traps/jnxVpnIfUp");
        alarm2.setLastEvent(event2);
        alarm2.setSeverityId(3);
        alarm2.setDistPoller(poller);
        alarm2.setCounter(100);
        alarm2.setLastEvent(event2);
        
        alarmDao.save(alarm2);
        alarmDao.flush();
        
        NegativeEventParmLikeFilter parmFilter = new NegativeEventParmLikeFilter("user=rtc");
        assertEquals("user is not \"rtc\"", parmFilter.getTextDescription());
        
        AlarmCriteria criteria = new AlarmCriteria(parmFilter);
        OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteria));
        
        // alarm2 and the alarm from DatabasePopulator match this criteria
        assertEquals(2, alarms.length);
    }

    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testSituations() {
        OnmsDistPoller poller = m_dbPopulator.getDistPollerDao().whoami();

        final OnmsEvent event = new OnmsEvent();
        event.setEventLog("Y");
        event.setEventDisplay("Y");
        event.setEventCreateTime(new Date());
        event.setDistPoller(poller);
        event.setEventTime(new Date());
        event.setEventSeverity(OnmsSeverity.CRITICAL.getId());
        event.setEventUei("uei://org/opennms/test/EventDaoTest");
        event.setEventSource("test");
        m_dbPopulator.getEventDao().save(event);
        m_dbPopulator.getEventDao().flush();

        final OnmsNode node = m_dbPopulator.getNodeDao().findAll().iterator().next();

        final OnmsAlarm alarm1 = new OnmsAlarm();
        alarm1.setNode(node);
        alarm1.setUei(event.getEventUei());
        alarm1.setSeverityId(event.getEventSeverity());
        alarm1.setFirstEventTime(event.getEventTime());
        alarm1.setLastEvent(event);
        alarm1.setCounter(1);
        alarm1.setDistPoller(poller);
        m_dbPopulator.getAlarmDao().save(alarm1);
        m_dbPopulator.getAlarmDao().flush();

        final OnmsAlarm alarm2 = new OnmsAlarm();
        alarm2.setNode(node);
        alarm2.setUei(event.getEventUei());
        alarm2.setSeverityId(event.getEventSeverity());
        alarm2.setFirstEventTime(event.getEventTime());
        alarm2.setLastEvent(event);
        alarm2.setCounter(1);
        alarm2.setDistPoller(poller);
        alarm2.setRelatedAlarms(Sets.newHashSet(alarm1));
        m_dbPopulator.getAlarmDao().save(alarm2);
        m_dbPopulator.getAlarmDao().flush();

        assertEquals(3, m_dbPopulator.getAlarmDao().findAll().size());

        AlarmCriteria criteriaForSituations = new AlarmCriteria(new SituationFilter(true));
        AlarmCriteria criteriaForAlarms = new AlarmCriteria(new SituationFilter(false));

        final OnmsAlarm[] situations = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteriaForSituations));
        final OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(criteriaForAlarms));

        assertEquals(1, situations.length);
        assertEquals(2, alarms.length);
    }

    public void checkFilteringAndSorting(final SortStyle sortStyle, final List<Filter> filterList, final int limit, final int multiple, final int expectedCount, final int expectedResults) {
        final AlarmQueryParms parms = new AlarmQueryParms();
        parms.ackType = AcknowledgeType.UNACKNOWLEDGED;
        parms.display = null;
        parms.filters = filterList;
        parms.limit = limit;
        parms.multiple = multiple;
        // apply the sort style
        parms.sortStyle = sortStyle;

        final AlarmCriteria queryCriteria = new AlarmCriteria(parms);
        final AlarmCriteria countCriteria = new AlarmCriteria(filterList, AcknowledgeType.UNACKNOWLEDGED);

        // get alarm entities
        final OnmsAlarm[] alarms = m_daoAlarmRepo.getMatchingAlarms(AlarmUtil.getOnmsCriteria(queryCriteria));
        // get alarm count
        final long alarmCount = m_daoAlarmRepo.countMatchingAlarms(AlarmUtil.getOnmsCriteria(countCriteria));

        // check expected values
        assertEquals(expectedCount, alarmCount);
        assertEquals(expectedResults, alarms.length);
        // check that the entity is populated with values
        assertNotNull(alarms[0].getId());
    }

    @Test
    @Transactional
    @JUnitTemporaryDatabase
    public void testFilteringAndSorting() {
        final OnmsDistPoller poller = m_dbPopulator.getDistPollerDao().whoami();

        // set up alarms...
        final OnmsEvent event = new OnmsEvent();
        event.setEventLog("Y");
        event.setEventDisplay("Y");
        event.setEventCreateTime(new Date());
        event.setDistPoller(poller);
        event.setEventTime(new Date());
        event.setEventSeverity(OnmsSeverity.CRITICAL.getId());
        event.setEventUei("uei://org/opennms/test/EventDaoTest");
        event.setEventSource("test");
        m_dbPopulator.getEventDao().save(event);
        m_dbPopulator.getEventDao().flush();

        final OnmsNode node = m_dbPopulator.getNodeDao().findAll().iterator().next();

        final OnmsAlarm alarm1 = new OnmsAlarm();
        alarm1.setNode(node);
        alarm1.setUei(event.getEventUei());
        alarm1.setSeverityId(event.getEventSeverity());
        alarm1.setFirstEventTime(event.getEventTime());
        alarm1.setLastEvent(event);
        alarm1.setCounter(1);
        alarm1.setDistPoller(poller);
        m_dbPopulator.getAlarmDao().save(alarm1);
        m_dbPopulator.getAlarmDao().flush();

        final OnmsAlarm alarm2 = new OnmsAlarm();
        alarm2.setNode(node);
        alarm2.setUei(event.getEventUei());
        alarm2.setSeverityId(event.getEventSeverity());
        alarm2.setFirstEventTime(event.getEventTime());
        alarm2.setLastEvent(event);
        alarm2.setCounter(1);
        alarm2.setDistPoller(poller);
        m_dbPopulator.getAlarmDao().save(alarm2);
        m_dbPopulator.getAlarmDao().flush();

        // ...and one situation
        final OnmsAlarm alarm3 = new OnmsAlarm();
        alarm3.setNode(node);
        alarm3.setUei(event.getEventUei());
        alarm3.setSeverityId(event.getEventSeverity());
        alarm3.setFirstEventTime(event.getEventTime());
        alarm3.setLastEvent(event);
        alarm3.setCounter(1);
        alarm3.setDistPoller(poller);
        alarm2.setRelatedAlarms(Sets.newHashSet(alarm1, alarm2));
        m_dbPopulator.getAlarmDao().save(alarm3);
        m_dbPopulator.getAlarmDao().flush();

        // some more alarms
        for(int i=0;i<10; i++) {
            final OnmsAlarm alarm = new OnmsAlarm();
            alarm.setNode(node);
            alarm.setUei(event.getEventUei());
            alarm.setSeverityId(event.getEventSeverity());
            alarm.setFirstEventTime(event.getEventTime());
            alarm.setLastEvent(event);
            alarm.setCounter(1);
            alarm.setDistPoller(poller);
            m_dbPopulator.getAlarmDao().save(alarm);
            m_dbPopulator.getAlarmDao().flush();
        }

        // 14 in total
        assertEquals(14, m_dbPopulator.getAlarmDao().findAll().size());

        // check for the single situation
        checkFilteringAndSorting(SortStyle.ID, Lists.newArrayList(new SituationFilter(true)), 10, 0, 1, 1);
        // check for all, first page of two
        checkFilteringAndSorting(SortStyle.ID, Lists.newArrayList(), 10, 0, 14, 10);
        // check for second page
        checkFilteringAndSorting(SortStyle.ID, Lists.newArrayList(), 10, 1, 14, 4);
        // check that all the sort styles work
        for(SortStyle sortStyle : SortStyle.values()) {
            checkFilteringAndSorting(sortStyle, Lists.newArrayList(new SituationFilter(true)), 10, 0, 1, 1);
        }
    }
    
    private AlarmCriteria getCriteria(Filter...filters){
        return new AlarmCriteria(filters);
    }
}
