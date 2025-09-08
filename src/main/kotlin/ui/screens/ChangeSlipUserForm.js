import React, { useEffect, useState } from "react";
import Select from "react-select";
import { axiosPrivate } from "../../utils/constants/api/axiosPrivate";
import { useDispatch } from "react-redux";
import Cookies from "universal-cookie";
import { toast } from "react-toastify";
import { closeModal } from "../../redux/features/modalSlice";

const ChangeSlipUserForm = (params) => {
  let [agents, setAgents] = useState([])
  let [agent, setAgent] = useState()
  let [loading, setLoading] = useState(false)

  let agentList = params.agentList
  let defaultUser = params.agent
  let termId = params.termId
  let slip = params.slip
  console.log(slip)
  let refreshSlip = params.refreshSlip

  const dispatch = useDispatch();
  const cookie = new Cookies();
  const token = cookie.get("token");

  useEffect(() => {
    if (agentList && agentList.length > 0) {
      console.log(agentList)
      let tmpAgentList = agentList.filter(agent => agent.value != "")
      let tmpAgList = tmpAgentList.map((agent) => {
        return { "label": agent.label, "value": agent.value }
      })
      setAgent(defaultUser)
      setAgents(tmpAgList)
    }
  }, [agentList])

  const handleFormSubmit = async () => {
    try {
      const u = agentList.find((v)=>v.value==agent)
      const payload = {
        slipId: slip.slipId,
        termId: termId,
        userId: agent,
        uid:slip.userId,
        userType:u?.userType
      };

      const response = await axiosPrivate.put("/v1/slip/updateUserChangeBySlipId", payload, {
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      });
      if (response.data.code == "200") {
        refreshSlip(Math.random())
        toast.success(response.data?.message);
      } else {
        toast.error(response.data?.message);
      }
      dispatch(closeModal())
    } catch (error) {

    }
  };

  const handleUserChange = (selectedOption) => {
    setAgent(selectedOption.value)
  }

  return (
    <form>
      <h2 className="text-2xl font-semibold mb-4 text-center text-gray-800">Agent List</h2>
      {agents && agents.length > 0 && agent ? (
        <Select
          placeholder="Select an agent"
          value={agents.find((opt) => opt.value === agent)}
          options={agents}
          onChange={handleUserChange}
          className="mt-2"
          menuPortalTarget={document.body} // Attempt to portal the menu to the body
          styles={{
            menuPortal: (base) => ({ ...base, zIndex: 9999 }), // Ensure the menu appears above other elements
          }}
        />
      ) : (
        <p className="text-gray-500">No agents available.</p>
      )}
      <div className="text-center mt-6">
        <button
          type="button"
          onClick={handleFormSubmit}
          className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-400"
        >
          Update
        </button>
      </div>
    </form>
  );
};

export default ChangeSlipUserForm;
