/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.shell.service.impl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.Before;
import org.junit.Test;
import org.obiba.magma.audit.UserProvider;
import org.obiba.opal.shell.CommandJob;
import org.obiba.opal.shell.OpalShell;
import org.obiba.opal.shell.commands.Command;
import org.obiba.opal.shell.service.impl.DefaultCommandJobService.FutureCommandJob;
import org.obiba.opal.web.model.Commands.CommandStateDto.Status;

/**
 * Unit tests for {@link DefaultCommandJobService}.
 */
public class DefaultCommandJobServiceTest {
  //
  // Instance Variables
  //

  private DefaultCommandJobService sut;

  private Executor mockExecutor;

  private UserProvider mockUserProvider;

  private CommandJob commandJob;

  private List<FutureCommandJob> futureCommandJobs;

  //
  // Fixture Methods (setUp / tearDown)
  //

  @Before
  public void setUp() {
    Command<?> cmd = createMock(Command.class);
    cmd.setShell((OpalShell) EasyMock.anyObject());
    expectLastCall().once();
    commandJob = new CommandJob(cmd);

    // Expect commandJob executed once.
    mockExecutor = createMock(Executor.class);
    mockExecutor.execute(eqFutureCommandJob(new FutureCommandJob(commandJob)));
    expectLastCall().once();

    // Expect current user is "testUser".
    mockUserProvider = createMock(UserProvider.class);
    expect(mockUserProvider.getUsername()).andReturn("testUser").atLeastOnce();

    sut = new DefaultCommandJobService() {
      protected List<FutureCommandJob> getFutureCommandJobs() {
        return futureCommandJobs != null ? futureCommandJobs : super.getFutureCommandJobs();
      }
    };
    sut.setExecutor(mockExecutor);
    sut.setUserProvider(mockUserProvider);

    replay(cmd, mockExecutor, mockUserProvider);
  }

  //
  // Test Methods
  //

  @Test
  public void testNotRunning() {
    assertEquals(false, sut.isRunning());
  }

  @Test
  public void testStart() {
    // Exercise
    sut.start();

    // Verify
    assertEquals(true, sut.isRunning());
  }

  @Test
  public void testStop() {
    // Exercise
    sut.stop();

    // Verify
    assertEquals(false, sut.isRunning());
  }

  @Test
  public void testGetCommand() {
    // Test-specific setup
    DefaultCommandJobService sut = new DefaultCommandJobService() {
      @Override
      public List<CommandJob> getHistory() {
        return createJobHistory();
      }
    };

    // Exercise
    Integer jobId = 2;
    CommandJob job = sut.getCommand(jobId);

    // Verify
    assertNotNull(job);
    assertEquals(jobId, job.getId());
  }

  @Test
  public void testGetCommand_ReturnsNullIfJobDoesNotExist() {
    // Test-specific setup
    DefaultCommandJobService sut = new DefaultCommandJobService() {
      @Override
      public List<CommandJob> getHistory() {
        return createJobHistory();
      }
    };

    // Exercise
    Integer bogusJobId = 99;
    CommandJob job = sut.getCommand(bogusJobId);

    // Verify
    assertNull(job);
  }

  @Test
  public void testLaunchCommand_AssignsIdToCommandJob() {
    sut.launchCommand(commandJob);

    assertNotNull(commandJob.getId());
  }

  @Test
  public void testLaunchCommand_MakesTheCurrentUserTheOwnerOfTheCommandJob() {
    sut.launchCommand(commandJob);

    verify(mockExecutor);

    assertEquals("testUser", commandJob.getOwner());
  }

  @Test
  public void testLaunchCommand_ExecutesCommandJob() {
    sut.launchCommand(commandJob);

    verify(mockExecutor);
  }

  @Test
  public void testGetHistory_ReturnsAllJobsInReverseOrderOfSubmission() {
    // Test-specific setup
    futureCommandJobs = new ArrayList<FutureCommandJob>();
    futureCommandJobs.add(new FutureCommandJob(createCommandJob(1, new Date(1))));
    futureCommandJobs.add(new FutureCommandJob(createCommandJob(2, new Date(2))));
    futureCommandJobs.add(new FutureCommandJob(createCommandJob(3, new Date(3))));

    // Exercise
    List<CommandJob> history = sut.getHistory();

    // Verify
    assertNotNull(history);
    assertEquals(3, history.size());
    assertEquals((Integer) 3, history.get(0).getId()); // job 3 first, since it was submitted last
    assertEquals((Integer) 2, history.get(1).getId()); // then job 2
    assertEquals((Integer) 1, history.get(2).getId()); // then job 1
  }

  @Test
  public void testCancelCommand_ChangesJobStatusToCancelPending() {
    // Test-specific setup
    initCommandJob(Status.IN_PROGRESS);

    // Exercise
    sut.cancelCommand(commandJob.getId());

    // Verify
    assertEquals(Status.CANCEL_PENDING, commandJob.getStatus());
  }

  @Test(expected = IllegalStateException.class)
  public void testCancelCommand_ThrowsIllegalStateExceptionIfCommandInSucceededState() {
    initCommandJob(Status.SUCCEEDED);

    sut.cancelCommand(commandJob.getId());
  }

  @Test(expected = IllegalStateException.class)
  public void testCancelCommand_ThrowsIllegalStateExceptionIfCommandInFailedState() {
    initCommandJob(Status.FAILED);

    sut.cancelCommand(commandJob.getId());
  }

  @Test(expected = IllegalStateException.class)
  public void testCancelCommand_ThrowsIllegalStateExceptionIfCommandInCanceledState() {
    initCommandJob(Status.CANCELED);

    sut.cancelCommand(commandJob.getId());
  }

  @Test(expected = IllegalStateException.class)
  public void testDeleteCommand_ThrowsIllegalStateExceptionIfCommandInNotStartedState() {
    initCommandJob(Status.NOT_STARTED);

    sut.deleteCommand(commandJob.getId());
  }

  @Test(expected = IllegalStateException.class)
  public void testDeleteCommand_ThrowsIllegalStateExceptionIfCommandInInProgressState() {
    initCommandJob(Status.IN_PROGRESS);

    sut.deleteCommand(commandJob.getId());
  }

  @Test(expected = IllegalStateException.class)
  public void testDeleteCommand_ThrowsIllegalStateExceptionIfCommandInCancelPendingState() {
    initCommandJob(Status.CANCEL_PENDING);

    sut.deleteCommand(commandJob.getId());
  }

  //
  // Helper Methods
  //

  private List<CommandJob> createJobHistory() {
    List<CommandJob> jobHistory = new ArrayList<CommandJob>();

    jobHistory.add(createCommandJob(1, new Date(1l)));
    jobHistory.add(createCommandJob(2, new Date(2l)));
    jobHistory.add(createCommandJob(3, new Date(3l)));

    return jobHistory;
  }

  private CommandJob createCommandJob(Integer id, Date submitTime) {
    Command<?> cmd = createMock(Command.class);
    cmd.setShell((OpalShell) EasyMock.anyObject());
    expectLastCall().once();

    CommandJob aCommandJob = new CommandJob(cmd);
    aCommandJob.setId(id);
    aCommandJob.setSubmitTime(submitTime);

    EasyMock.replay(cmd);
    return aCommandJob;
  }

  private void initCommandJob(Status status) {
    commandJob.setId(1);
    commandJob.setOwner("testUser");
    commandJob.setSubmitTime(new Date());
    commandJob.setStatus(status);

    futureCommandJobs = new ArrayList<FutureCommandJob>();
    futureCommandJobs.add(new FutureCommandJob(commandJob));
  }

  //
  // Inner Classes
  //

  static class FutureCommandJobMatcher implements IArgumentMatcher {

    private FutureCommandJob expected;

    public FutureCommandJobMatcher(FutureCommandJob expected) {
      this.expected = expected;
    }

    @Override
    public boolean matches(Object actual) {
      if(actual instanceof FutureCommandJob) {
        return ((FutureCommandJob) actual).getCommandJob().equals(expected.getCommandJob());
      } else {
        return false;
      }
    }

    @Override
    public void appendTo(StringBuffer buffer) {
      buffer.append("eqFutureCommandJob(");
      buffer.append(expected.getClass().getName());
      buffer.append(" with commandJob \"");
      buffer.append(expected.getCommandJob());
      buffer.append("\")");
    }

  }

  static FutureCommandJob eqFutureCommandJob(FutureCommandJob in) {
    EasyMock.reportMatcher(new FutureCommandJobMatcher(in));
    return null;
  }
}
